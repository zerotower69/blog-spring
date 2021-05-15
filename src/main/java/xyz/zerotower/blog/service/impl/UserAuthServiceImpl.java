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
     * 邮箱号
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
     * github 回调地址
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
     * gitee 回调地址
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
     * 发送邮件验证码
     * @param username 邮箱号
     */
    @Override
    public void sendCode(String username) {
        // 校验账号是否合法
        if (!checkEmail(username)) {
            throw new ServeException("请输入正确邮箱");
        }
        // 生成六位随机验证码发送
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(email);
        message.setTo(username);
        message.setSubject("验证码");
        message.setText("您的验证码为 " + code.toString() + " 有效期5分钟，请不要告诉他人哦！");
        javaMailSender.send(message);
        // 将验证码存入redis，设置过期时间为15分钟
        redisTemplate.boundValueOps(CODE_KEY + username).set(code);
        redisTemplate.expire(CODE_KEY + username, CODE_EXPIRE_TIME, TimeUnit.MILLISECONDS);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveUser(UserVO user) {
        // 校验账号是否合法
        if (checkUser(user)) {
            throw new ServeException("邮箱已被注册！");
        }
        // 新增用户信息
        UserInfo userInfo = UserInfo.builder()
                .nickname(CommonConst.DEFAULT_NICKNAME)
                .avatar(CommonConst.DEFAULT_AVATAR)
                .createTime(new Date())
                .build();
        userInfoDao.insert(userInfo);
        // 绑定用户角色
        saveUserRole(userInfo);
        // 新增用户账号
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
        // 校验账号是否合法
        if (!checkUser(user)) {
            throw new ServeException("邮箱尚未注册！");
        }
        // 根据用户名修改密码
        userAuthDao.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
                .set(UserAuth::getPassword, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()))
                .eq(UserAuth::getUsername, user.getUsername()));
    }

    /**
     * 修改管理员的密码
     * @param passwordVO 密码对象
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAdminPassword(PasswordVO passwordVO) {
        // 查询旧密码是否正确
        UserAuth user = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getId, UserUtil.getLoginUser().getId()));
        // 正确则修改密码，错误则提示不正确
        if (Objects.nonNull(user) && BCrypt.checkpw(passwordVO.getOldPassword(), user.getPassword())) {
            UserAuth userAuth = UserAuth.builder()
                    .id(UserUtil.getLoginUser().getId())
                    .password(BCrypt.hashpw(passwordVO.getNewPassword(), BCrypt.gensalt()))
                    .build();
            userAuthDao.updateById(userAuth);
        } else {
            throw new ServeException("旧密码不正确");
        }
    }

    @Override
    public PageDTO<UserBackDTO> listUserBackDTO(ConditionVO condition) {
        // 转换页码
        condition.setCurrent((condition.getCurrent() - 1) * condition.getSize());
        // 获取后台用户数量
        Integer count = userAuthDao.countUser(condition);
        if (count == 0) {
            return new PageDTO<>();
        }
        // 获取后台用户列表
        List<UserBackDTO> userBackDTOList = userAuthDao.listUsers(condition);
        return new PageDTO<>(userBackDTOList, count);
    }


    /**
     * 绑定用户角色
     *
     * @param userInfo 用户信息
     */
    private void saveUserRole(UserInfo userInfo) {
        UserRole userRole = UserRole.builder()
                .userId(userInfo.getId())
                .roleId(RoleEnum.USER.getRoleId())
                .build();
        userRoleDao.insert(userRole);
    }

    /**
     * github登录
     * @param code
     * @return  github账号信息
     * @throws IOException
     */
    @Transactional(rollbackFor = ServeException.class)
    @Override
    public UserInfoDTO githubLogin(String code) throws IOException {
        //创建登录信息
        UserInfoDTO userInfoDTO;
        // 用code换取accessToken和uid
        HttpClient client = HttpClients.createDefault();
        //创建post请求
        HttpPost post = new HttpPost(GITHUB_TOKEN_URI);
        //封装请求参数
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", GITHUB_CLIENT_ID));
        params.add(new BasicNameValuePair("client_secret", GITHUB_CLIENT_SECRET));
        params.add(new BasicNameValuePair("code", code));
        //参数实体转化
        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, "UTF-8");
        post.setEntity(urlEncodedFormEntity);
        post.addHeader("accept", "application/json");
        post.addHeader("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Mobile Safari/537.36");
        //2.尝试去请求
        HttpResponse githubResponse = client.execute(post);  //执行这个post请求
        int statusCode = githubResponse.getStatusLine().getStatusCode();
        if(statusCode!=200){
            return null;
        }
        org.apache.http.HttpEntity responseEntity = githubResponse.getEntity();  //获取响应实体
        String text = EntityUtils.toString(responseEntity);  //将实体转换为文本
            logger.info("获取的文本=\n" + text);
        JSONObject object = JSONObject.parseObject(text);  //文本转为json对象
        if(object.containsKey("error")){
            return null;
        }
        //获取 accessToken
        String accessToken=object.getString("access_token");
        //TODO
        //打印返回的信息
       logger.info("返回的信息\n"+object.toJSONString());
        //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        //3.请求获取用户的信息
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
        HttpResponse userResponse=  client2.execute(get); //执行请求
        statusCode=userResponse.getStatusLine().getStatusCode();  //获取返回的请求头
        //获得返回实体
        org.apache.http.HttpEntity userEntity=userResponse.getEntity();  //取实体
        String userText= EntityUtils.toString(userEntity);  //实体转文本
        JSONObject userJson= JSONObject.parseObject(userText); //转为json对象
        if(statusCode!=200 ||!userJson.containsKey("login"))
        {
            return null;
        }
            //获取id
            String uid=userJson.getString("id");
            UserAuth user = getUserAuth(uid, LoginTypeEnum.GITHUB.getType());
            if (Objects.nonNull(user) && Objects.nonNull(user.getUserInfoId())) {
                // 存在则返回数据库中的用户信息封装
                userInfoDTO = getUserInfoDTO(user);
            } else {
                // 获取ip地址
                String ipAddr = IpUtil.getIpAddr(request);
                String ipSource = IpUtil.getIpSource(ipAddr);
                // 将账号和信息存入数据库
                UserInfo userInfo = convertUserInfo(userJson.getString("login"),userJson.getString("avatar_url"));
                userInfoDao.insert(userInfo);
                UserAuth userAuth = convertUserAuth(userInfo.getId(), uid, accessToken, ipAddr, ipSource, LoginTypeEnum.GITHUB.getType());
                userAuthDao.insert(userAuth);
                // 绑定角色
                saveUserRole(userInfo);
                // 封装登录信息
                userInfoDTO = convertLoginUser(userAuth, userInfo, Lists.newArrayList(RoleEnum.USER.getLabel()),null,null,request);
            }
            // 将登录信息放入springSecurity管理
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userInfoDTO, null, userInfoDTO.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return userInfoDTO;
    }


    /**
     * gitee 登录
     * @param code
     * @return
     * @throws IOException
     */
    @Transactional(rollbackFor = ServeException.class)
    @Override
    public UserInfoDTO giteeLogin(String code) throws IOException{
        //创建登录信息
        UserInfoDTO userInfoDTO;
        // 用code换取accessToken和uid
        HttpClient client = HttpClients.createDefault();
        //创建post请求
        HttpPost post = new HttpPost(GITEE_TOKEN_URI);
        //封装请求参数
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type","authorization_code"));
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("client_id", GITEE_CLIENT_ID));
        params.add(new BasicNameValuePair("client_secret", GITEE_CLIENT_SECRET));
        params.add(new BasicNameValuePair("redirect_uri",GITEE_REDIRECT_URI));

        //发起请求了
        //参数实体转化
        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, "UTF-8");
        post.setEntity(urlEncodedFormEntity);
        post.addHeader("accept", "application/json");
        post.addHeader("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Mobile Safari/537.36");
        //2.尝试去请求
        HttpResponse githubResponse = client.execute(post);  //执行这个post请求
        int statusCode = githubResponse.getStatusLine().getStatusCode();
        if(statusCode!=200)
        {
            return null;
        }
        org.apache.http.HttpEntity responseEntity = githubResponse.getEntity();  //获取响应实体
        String text = EntityUtils.toString(responseEntity);  //将实体转换为文本
        logger.info("获取的文本=\n" + text);
        JSONObject object = JSONObject.parseObject(text);  //文本转为json对象
        logger.info("内容转换输出= " + object.toJSONString());  //测试打印输出
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
        HttpResponse userResponse=  client2.execute(get); //执行请求
        statusCode=userResponse.getStatusLine().getStatusCode();  //获取返回的请求头
        //获得返回实体
        org.apache.http.HttpEntity userEntity=userResponse.getEntity();  //取实体
        String userText= EntityUtils.toString(userEntity);  //实体转文本
        JSONObject userJson= JSONObject.parseObject(userText); //转为json对象
        //响应代码或者实体信息错误
        logger.info(userJson.toJSONString());
        logger.info("响应的代码="+String.valueOf(statusCode));
        if(statusCode!=200||userJson.getString("login")==null){
           return null;
        }
        //获取id
        String uid=userJson.getString("id");
        UserAuth user = getUserAuth(uid, LoginTypeEnum.GITEE.getType());
        if (Objects.nonNull(user) && Objects.nonNull(user.getUserInfoId())) {
            // 存在则返回数据库中的用户信息封装
            userInfoDTO = getUserInfoDTO(user);
        } else {
            // 获取ip地址
            String ipAddr = IpUtil.getIpAddr(request);
            String ipSource = IpUtil.getIpSource(ipAddr);
            // 将账号和信息存入数据库
            UserInfo userInfo = convertUserInfo(userJson.getString("login"),userJson.getString("avatar_url"));
            userInfoDao.insert(userInfo);
            UserAuth userAuth = convertUserAuth(userInfo.getId(), uid, accessToken, ipAddr, ipSource, LoginTypeEnum.GITEE.getType());
            userAuthDao.insert(userAuth);
            // 绑定角色
            saveUserRole(userInfo);
            // 封装登录信息
            userInfoDTO = convertLoginUser(userAuth, userInfo, Lists.newArrayList(RoleEnum.USER.getLabel()),null,null,request);
        }
        // 将登录信息放入springSecurity管理
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userInfoDTO, null, userInfoDTO.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return userInfoDTO;
    }

    /**
     * 封装用户信息
     *
     * @param nickname 昵称
     * @param avatar   头像
     * @return 用户信息
     */
    private UserInfo convertUserInfo(String nickname, String avatar) {
        return UserInfo.builder()
                .nickname(nickname)
                .avatar(avatar)
                .createTime(new Date())
                .build();
    }

    /**
     * 封装用户账号
     *
     * @param userInfoId  用户信息id
     * @param uid         唯一Id标识
     * @param accessToken 登录凭证
     * @param ipAddr      ip地址
     * @param ipSource    ip来源
     * @param loginType   登录方式
     * @return 用户账号
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
     * 获取本地第三方登录信息
     *
     * @param user 用户对象
     * @return 用户登录信息
     */
    private UserInfoDTO getUserInfoDTO(UserAuth user) {
        // 更新登录时间，ip
        String ipAddr = IpUtil.getIpAddr(request);
        String ipSource = IpUtil.getIpSource(ipAddr);
        userAuthDao.update(new UserAuth(), new LambdaUpdateWrapper<UserAuth>()
                .set(UserAuth::getLastLoginTime, new Date())
                .set(UserAuth::getIpAddr, ipAddr)
                .set(UserAuth::getIpSource, ipSource)
                .eq(UserAuth::getId, user.getId()));
        // 查询账号对应的信息
        UserInfo userInfo = userInfoDao.selectOne(new LambdaQueryWrapper<UserInfo>()
                .select(UserInfo::getId, UserInfo::getNickname, UserInfo::getAvatar, UserInfo::getIntro, UserInfo::getWebSite, UserInfo::getIsDisable)
                .eq(UserInfo::getId, user.getUserInfoId()));
        // 查询账号点赞信息
        Set<Integer> articleLikeSet = (Set<Integer>) redisTemplate.boundHashOps("article_user_like").get(userInfo.getId().toString());
        Set<Integer> commentLikeSet = (Set<Integer>) redisTemplate.boundHashOps("comment_user_like").get(userInfo.getId().toString());
        // 查询账号角色
        List<String> roleList = roleDao.listRolesByUserInfoId(userInfo.getId());
        // 封装信息
        return convertLoginUser(user, userInfo, roleList, articleLikeSet, commentLikeSet, request);
    }


    /**
     * 检测第三方账号是否注册
     *
     * @param openId    第三方唯一id
     * @param loginType 登录方式
     * @return 用户账号信息
     */
    private UserAuth getUserAuth(String openId, Integer loginType) {
        // 查询账号信息
        return userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getId, UserAuth::getUserInfoId)
                .eq(UserAuth::getUsername, openId)
                .eq(UserAuth::getLoginType, loginType));
    }

    /**
     * 检测邮箱是否合法
     *
     * @param username 用户名
     * @return 合法状态
     */
    private boolean checkEmail(String username) {
        String rule = "^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$";
        //正则表达式的模式 编译正则表达式
        Pattern p = Pattern.compile(rule);
        //正则表达式的匹配器
        Matcher m = p.matcher(username);
        //进行正则匹配
        return m.matches();
    }

    /**
     * 校验用户数据是否合法
     *
     * @param user 用户数据
     * @return 合法状态
     */
    private Boolean checkUser(UserVO user) {
        if (!user.getCode().equals(redisTemplate.boundValueOps(CODE_KEY + user.getUsername()).get())) {
            throw new ServeException("验证码错误！");
        }
        //查询用户名是否存在
        UserAuth userAuth = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getUsername).eq(UserAuth::getUsername, user.getUsername()));
        return Objects.nonNull(userAuth);
    }

}
