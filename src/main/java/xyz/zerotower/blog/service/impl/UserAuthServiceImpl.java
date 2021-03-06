package xyz.zerotower.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.zerotower.blog.constant.CommonConst;
import xyz.zerotower.blog.dao.RoleDao;
import xyz.zerotower.blog.dao.UserAuthDao;
import xyz.zerotower.blog.dao.UserInfoDao;
import xyz.zerotower.blog.dao.UserRoleDao;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.dto.UserBackDTO;
import xyz.zerotower.blog.dto.UserInfoDTO;
import xyz.zerotower.blog.entity.UserAuth;
import xyz.zerotower.blog.entity.UserInfo;
import xyz.zerotower.blog.entity.UserRole;
import xyz.zerotower.blog.enums.LoginTypeEnum;
import xyz.zerotower.blog.enums.RoleEnum;
import xyz.zerotower.blog.exception.ServeException;
import xyz.zerotower.blog.service.UserAuthService;
import xyz.zerotower.blog.utils.IpUtil;
import xyz.zerotower.blog.utils.UserUtil;
import xyz.zerotower.blog.vo.ConditionVO;
import xyz.zerotower.blog.vo.PasswordVO;
import xyz.zerotower.blog.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.zerotower.blog.constant.RedisPrefixConst.CODE_EXPIRE_TIME;
import static xyz.zerotower.blog.constant.RedisPrefixConst.CODE_KEY;
import static xyz.zerotower.blog.utils.UserUtil.convertLoginUser;

/**
 * @author xiaojie
 * @since 2020-05-18
 */
@Service
public class UserAuthServiceImpl extends ServiceImpl<UserAuthDao, UserAuth> implements UserAuthService {
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserAuthDao userAuthDao;
    @Autowired
    private UserRoleDao userRoleDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private RestTemplate restTemplate;
    @Resource
    private HttpServletRequest request;

    private Logger logger = LoggerFactory.getLogger(UserAuthServiceImpl.class);

    /**
     * ?????????
     */
    @Value("${spring.mail.username}")
    private String email;

    /**
     * github client id
     */
    @Value("${github.client.id}")
    private String GITHUB_CLIENT_ID;

    /**
     * github client secret
     */
    @Value("${github.client.secret}")
    private String GITHUB_CLIENT_SECRET;

    /**
     * github ????????????
     */
    @Value("${github.redirect_uri}")
    private String GITHUB_REDIRECT_URI;

    /**
     * github token uri
     */
    @Value("${github.token_uri}")
    private String GITHUB_TOKEN_URI;

    /**
     * github api uri
     */
    @Value("${github.api_uri}")
    private String GITHUB_API_URI;

    /**
     * gitee client id
     */
    @Value("${gitee.client.id}")
    private String GITEE_CLIENT_ID;

    /**
     * gitee client secret
     */
    @Value("${gitee.client.secret}")
    private String GITEE_CLIENT_SECRET;

    /**
     * gitee ????????????
     */
    @Value("${gitee.redirect_uri}")
    private String GITEE_REDIRECT_URI;

    /**
     * gitee token uri
     */
    @Value("${gitee.token_uri}")
    private String GITEE_TOKEN_URI;

    /**
     * gitee api uri
     */
    @Value("${gitee.api_uri}")
    private String GITEE_API_URI;


    /**
     * ?????????????????????
     * @param username ?????????
     */
    @Override
    public void sendCode(String username) {
        // ????????????????????????
        if (!checkEmail(username)) {
            throw new ServeException("?????????????????????");
        }
        // ?????????????????????????????????
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(email);
        message.setTo(username);
        message.setSubject("?????????");
        message.setText("?????????????????? " + code.toString() + " ?????????5????????????????????????????????????");
        javaMailSender.send(message);
        // ??????????????????redis????????????????????????15??????
        redisTemplate.boundValueOps(CODE_KEY + username).set(code);
        redisTemplate.expire(CODE_KEY + username, CODE_EXPIRE_TIME, TimeUnit.MILLISECONDS);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveUser(UserVO user) {
        // ????????????????????????
        if (checkUser(user)) {
            throw new ServeException("?????????????????????");
        }
        // ??????????????????
        UserInfo userInfo = UserInfo.builder()
                .nickname(CommonConst.DEFAULT_NICKNAME)
                .avatar(CommonConst.DEFAULT_AVATAR)
                .createTime(new Date())
                .build();
        userInfoDao.insert(userInfo);
        // ??????????????????
        saveUserRole(userInfo);
        // ??????????????????
        UserAuth userAuth = UserAuth.builder()
                .userInfoId(userInfo.getId())
                .username(user.getUsername())
                .password(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()))
                .createTime(new Date())
                .loginType(LoginTypeEnum.EMAIL.getType())
                .build();
        userAuthDao.insert(userAuth);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePassword(UserVO user) {
        // ????????????????????????
        if (!checkUser(user)) {
            throw new ServeException("?????????????????????");
        }
        // ???????????????????????????
        userAuthDao.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
                .set(UserAuth::getPassword, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()))
                .eq(UserAuth::getUsername, user.getUsername()));
    }

    /**
     * ????????????????????????
     * @param passwordVO ????????????
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAdminPassword(PasswordVO passwordVO) {
        // ???????????????????????????
        UserAuth user = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getId, UserUtil.getLoginUser().getId()));
        // ????????????????????????????????????????????????
        if (Objects.nonNull(user) && BCrypt.checkpw(passwordVO.getOldPassword(), user.getPassword())) {
            UserAuth userAuth = UserAuth.builder()
                    .id(UserUtil.getLoginUser().getId())
                    .password(BCrypt.hashpw(passwordVO.getNewPassword(), BCrypt.gensalt()))
                    .build();
            userAuthDao.updateById(userAuth);
        } else {
            throw new ServeException("??????????????????");
        }
    }

    @Override
    public PageDTO<UserBackDTO> listUserBackDTO(ConditionVO condition) {
        // ????????????
        condition.setCurrent((condition.getCurrent() - 1) * condition.getSize());
        // ????????????????????????
        Integer count = userAuthDao.countUser(condition);
        if (count == 0) {
            return new PageDTO<>();
        }
        // ????????????????????????
        List<UserBackDTO> userBackDTOList = userAuthDao.listUsers(condition);
        return new PageDTO<>(userBackDTOList, count);
    }


    /**
     * ??????????????????
     *
     * @param userInfo ????????????
     */
    private void saveUserRole(UserInfo userInfo) {
        UserRole userRole = UserRole.builder()
                .userId(userInfo.getId())
                .roleId(RoleEnum.USER.getRoleId())
                .build();
        userRoleDao.insert(userRole);
    }

    /**
     * github??????
     * @param code
     * @return  github????????????
     * @throws IOException
     */
    @Transactional(rollbackFor = ServeException.class)
    @Override
    public UserInfoDTO githubLogin(String code) throws IOException {
        //??????????????????
        UserInfoDTO userInfoDTO;
        // ???code??????accessToken???uid
        HttpClient client = HttpClients.createDefault();
        //??????post??????
        HttpPost post = new HttpPost(GITHUB_TOKEN_URI);
        //??????????????????
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", GITHUB_CLIENT_ID));
        params.add(new BasicNameValuePair("client_secret", GITHUB_CLIENT_SECRET));
        params.add(new BasicNameValuePair("code", code));
        //??????????????????
        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, "UTF-8");
        post.setEntity(urlEncodedFormEntity);
        post.addHeader("accept", "application/json");
        post.addHeader("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Mobile Safari/537.36");
        //2.???????????????
        HttpResponse githubResponse = client.execute(post);  //????????????post??????
        int statusCode = githubResponse.getStatusLine().getStatusCode();
        if(statusCode!=200){
            return null;
        }
        org.apache.http.HttpEntity responseEntity = githubResponse.getEntity();  //??????????????????
        String text = EntityUtils.toString(responseEntity);  //????????????????????????
            logger.info("???????????????=\n" + text);
        JSONObject object = JSONObject.parseObject(text);  //????????????json??????
        if(object.containsKey("error")){
            return null;
        }
        //?????? accessToken
        String accessToken=object.getString("access_token");
        //TODO
        //?????????????????????
       logger.info("???????????????\n"+object.toJSONString());
        //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        //3.???????????????????????????
        HttpClient client2= HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(true)
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .build();
        HttpGet get = new HttpGet(GITHUB_API_URI+ accessToken);
        get.setConfig(requestConfig);
        get.setHeader("Authorization", "token " + accessToken);
        get.setHeader("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Mobile Safari/537.36");
//
        HttpResponse userResponse=  client2.execute(get); //????????????
        statusCode=userResponse.getStatusLine().getStatusCode();  //????????????????????????
        //??????????????????
        org.apache.http.HttpEntity userEntity=userResponse.getEntity();  //?????????
        String userText= EntityUtils.toString(userEntity);  //???????????????
        JSONObject userJson= JSONObject.parseObject(userText); //??????json??????
        if(statusCode!=200 ||!userJson.containsKey("login"))
        {
            return null;
        }
            //??????id
            String uid=userJson.getString("id");
            UserAuth user = getUserAuth(uid, LoginTypeEnum.GITHUB.getType());
            if (Objects.nonNull(user) && Objects.nonNull(user.getUserInfoId())) {
                // ????????????????????????????????????????????????
                userInfoDTO = getUserInfoDTO(user);
            } else {
                // ??????ip??????
                String ipAddr = IpUtil.getIpAddr(request);
                String ipSource = IpUtil.getIpSource(ipAddr);
                // ?????????????????????????????????
                UserInfo userInfo = convertUserInfo(userJson.getString("login"),userJson.getString("avatar_url"));
                userInfoDao.insert(userInfo);
                UserAuth userAuth = convertUserAuth(userInfo.getId(), uid, accessToken, ipAddr, ipSource, LoginTypeEnum.GITHUB.getType());
                userAuthDao.insert(userAuth);
                // ????????????
                saveUserRole(userInfo);
                // ??????????????????
                userInfoDTO = convertLoginUser(userAuth, userInfo, Lists.newArrayList(RoleEnum.USER.getLabel()),null,null,request);
            }
            // ?????????????????????springSecurity??????
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userInfoDTO, null, userInfoDTO.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return userInfoDTO;
    }


    /**
     * gitee ??????
     * @param code
     * @return
     * @throws IOException
     */
    @Transactional(rollbackFor = ServeException.class)
    @Override
    public UserInfoDTO giteeLogin(String code) throws IOException{
        //??????????????????
        UserInfoDTO userInfoDTO;
        // ???code??????accessToken???uid
        HttpClient client = HttpClients.createDefault();
        //??????post??????
        HttpPost post = new HttpPost(GITEE_TOKEN_URI);
        //??????????????????
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type","authorization_code"));
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("client_id", GITEE_CLIENT_ID));
        params.add(new BasicNameValuePair("client_secret", GITEE_CLIENT_SECRET));
        params.add(new BasicNameValuePair("redirect_uri",GITEE_REDIRECT_URI));

        //???????????????
        //??????????????????
        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, "UTF-8");
        post.setEntity(urlEncodedFormEntity);
        post.addHeader("accept", "application/json");
        post.addHeader("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Mobile Safari/537.36");
        //2.???????????????
        HttpResponse githubResponse = client.execute(post);  //????????????post??????
        int statusCode = githubResponse.getStatusLine().getStatusCode();
        if(statusCode!=200)
        {
            return null;
        }
        org.apache.http.HttpEntity responseEntity = githubResponse.getEntity();  //??????????????????
        String text = EntityUtils.toString(responseEntity);  //????????????????????????
        logger.info("???????????????=\n" + text);
        JSONObject object = JSONObject.parseObject(text);  //????????????json??????
        logger.info("??????????????????= " + object.toJSONString());  //??????????????????
        if(object.getString("error")!=null){
            return null;
        }
        String accessToken=object.getString("access_token");
        HttpClient client2= HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(true)
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .build();
        HttpGet get = new HttpGet(GITEE_API_URI+"?access_token="+accessToken);
        get.setConfig(requestConfig);
        get.setHeader("Authorization", "token " + accessToken);
        get.setHeader("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Mobile Safari/537.36");
//
        HttpResponse userResponse=  client2.execute(get); //????????????
        statusCode=userResponse.getStatusLine().getStatusCode();  //????????????????????????
        //??????????????????
        org.apache.http.HttpEntity userEntity=userResponse.getEntity();  //?????????
        String userText= EntityUtils.toString(userEntity);  //???????????????
        JSONObject userJson= JSONObject.parseObject(userText); //??????json??????
        //????????????????????????????????????
        logger.info(userJson.toJSONString());
        logger.info("???????????????="+String.valueOf(statusCode));
        if(statusCode!=200||userJson.getString("login")==null){
           return null;
        }
        //??????id
        String uid=userJson.getString("id");
        UserAuth user = getUserAuth(uid, LoginTypeEnum.GITEE.getType());
        if (Objects.nonNull(user) && Objects.nonNull(user.getUserInfoId())) {
            // ????????????????????????????????????????????????
            userInfoDTO = getUserInfoDTO(user);
        } else {
            // ??????ip??????
            String ipAddr = IpUtil.getIpAddr(request);
            String ipSource = IpUtil.getIpSource(ipAddr);
            // ?????????????????????????????????
            UserInfo userInfo = convertUserInfo(userJson.getString("login"),userJson.getString("avatar_url"));
            userInfoDao.insert(userInfo);
            UserAuth userAuth = convertUserAuth(userInfo.getId(), uid, accessToken, ipAddr, ipSource, LoginTypeEnum.GITEE.getType());
            userAuthDao.insert(userAuth);
            // ????????????
            saveUserRole(userInfo);
            // ??????????????????
            userInfoDTO = convertLoginUser(userAuth, userInfo, Lists.newArrayList(RoleEnum.USER.getLabel()),null,null,request);
        }
        // ?????????????????????springSecurity??????
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userInfoDTO, null, userInfoDTO.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return userInfoDTO;
    }

    /**
     * ??????????????????
     *
     * @param nickname ??????
     * @param avatar   ??????
     * @return ????????????
     */
    private UserInfo convertUserInfo(String nickname, String avatar) {
        return UserInfo.builder()
                .nickname(nickname)
                .avatar(avatar)
                .createTime(new Date())
                .build();
    }

    /**
     * ??????????????????
     *
     * @param userInfoId  ????????????id
     * @param uid         ??????Id??????
     * @param accessToken ????????????
     * @param ipAddr      ip??????
     * @param ipSource    ip??????
     * @param loginType   ????????????
     * @return ????????????
     */
    private UserAuth convertUserAuth(Integer userInfoId, String uid, String accessToken, String ipAddr, String ipSource, Integer loginType) {
        return UserAuth.builder()
                .userInfoId(userInfoId)
                .username(uid)
                .password(accessToken)
                .loginType(loginType)
                .ipAddr(ipAddr)
                .ipSource(ipSource)
                .createTime(new Date())
                .lastLoginTime(new Date())
                .build();
    }

    /**
     * ?????????????????????????????????
     *
     * @param user ????????????
     * @return ??????????????????
     */
    private UserInfoDTO getUserInfoDTO(UserAuth user) {
        // ?????????????????????ip
        String ipAddr = IpUtil.getIpAddr(request);
        String ipSource = IpUtil.getIpSource(ipAddr);
        userAuthDao.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
                .set(UserAuth::getLastLoginTime, new Date())
                .set(UserAuth::getIpAddr, ipAddr)
                .set(UserAuth::getIpSource, ipSource)
                .eq(UserAuth::getId, user.getId()));
        // ???????????????????????????
        UserInfo userInfo = userInfoDao.selectOne(new LambdaQueryWrapper<UserInfo>()
                .select(UserInfo::getId, UserInfo::getNickname, UserInfo::getAvatar, UserInfo::getIntro, UserInfo::getWebSite, UserInfo::getIsDisable)
                .eq(UserInfo::getId, user.getUserInfoId()));
        // ????????????????????????
        Set<Integer> articleLikeSet = (Set<Integer>) redisTemplate.boundHashOps("article_user_like").get(userInfo.getId().toString());
        Set<Integer> commentLikeSet = (Set<Integer>) redisTemplate.boundHashOps("comment_user_like").get(userInfo.getId().toString());
        // ??????????????????
        List<String> roleList = roleDao.listRolesByUserInfoId(userInfo.getId());
        // ????????????
        return convertLoginUser(user, userInfo, roleList, articleLikeSet, commentLikeSet, request);
    }


    /**
     * ?????????????????????????????????
     *
     * @param openId    ???????????????id
     * @param loginType ????????????
     * @return ??????????????????
     */
    private UserAuth getUserAuth(String openId, Integer loginType) {
        // ??????????????????
        return userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getId, UserAuth::getUserInfoId)
                .eq(UserAuth::getUsername, openId)
                .eq(UserAuth::getLoginType, loginType));
    }

    /**
     * ????????????????????????
     *
     * @param username ?????????
     * @return ????????????
     */
    private boolean checkEmail(String username) {
        String rule = "^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$";
        //???????????????????????? ?????????????????????
        Pattern p = Pattern.compile(rule);
        //???????????????????????????
        Matcher m = p.matcher(username);
        //??????????????????
        return m.matches();
    }

    /**
     * ??????????????????????????????
     *
     * @param user ????????????
     * @return ????????????
     */
    private Boolean checkUser(UserVO user) {
        if (!user.getCode().equals(redisTemplate.boundValueOps(CODE_KEY + user.getUsername()).get())) {
            throw new ServeException("??????????????????");
        }
        //???????????????????????????
        UserAuth userAuth = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getUsername).eq(UserAuth::getUsername, user.getUsername()));
        return Objects.nonNull(userAuth);
    }

}
