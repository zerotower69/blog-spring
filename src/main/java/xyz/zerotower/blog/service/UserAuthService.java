package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.dto.UserBackDTO;
import xyz.zerotower.blog.dto.UserInfoDTO;
import xyz.zerotower.blog.entity.UserAuth;
import xyz.zerotower.blog.vo.ConditionVO;
import xyz.zerotower.blog.vo.PasswordVO;
import xyz.zerotower.blog.vo.UserVO;

import java.io.IOException;
import java.io.UnsupportedEncodingException;


/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
public interface UserAuthService extends IService<UserAuth> {

    /**
     * 发送邮箱验证码
     *
     * @param username 邮箱号
     */
    void sendCode(String username);

    /**
     * 用户注册
     *
     * @param user 用户对象
     */
    void saveUser(UserVO user);

    /**
     * github登录
     *
     * @param code
     * @return  用户登录信息
     */
    UserInfoDTO githubLogin(String code) throws IOException;

    /**
     * gitee 登录
     * @param code
     * @return 用户登录信息
     * @throws IOException
     */
    UserInfoDTO giteeLogin(String code) throws IOException;

    /**
     * 修改密码
     *
     * @param user 用户对象
     */
    void updatePassword(UserVO user);

    /**
     * 修改管理员密码
     *
     * @param passwordVO 密码对象
     */
    void updateAdminPassword(PasswordVO passwordVO);

    /**
     * 查询后台用户列表
     *
     * @param condition 条件
     * @return 用户列表
     */
    PageDTO<UserBackDTO> listUserBackDTO(ConditionVO condition);

}
