package xyz.zerotower.blog.controller;


import xyz.zerotower.blog.constant.StatusConst;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.dto.UserBackDTO;
import xyz.zerotower.blog.dto.UserInfoDTO;
import xyz.zerotower.blog.service.UserAuthService;
import xyz.zerotower.blog.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.sql.ResultSet;

/***
 * @author xiaojie
 * @since 2020-05-18
 */
@Api(tags = "用户账号模块")
@RestController
public class UserAuthController {
    @Autowired
    private UserAuthService userAuthService;

    @ApiOperation(value = "发送邮箱验证码")
    @ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "String")
    @GetMapping("/users/code")
    public Result sendCode(String username) {
        userAuthService.sendCode(username);
        return new Result<>(true, StatusConst.OK, "发送成功！");
    }

    @ApiOperation(value = "查看后台用户列表")
    @GetMapping("/admin/users")
    public Result<PageDTO<UserBackDTO>> listUsers(ConditionVO condition) {
        return new Result<>(true, StatusConst.OK, "查询成功！", userAuthService.listUserBackDTO(condition));
    }

    @ApiOperation(value = "用户注册")
    @PostMapping("/users")
    public Result saveUser(@Valid @RequestBody UserVO user) {
        userAuthService.saveUser(user);
        return new Result<>(true, StatusConst.OK, "注册成功！");
    }

    @ApiOperation(value = "修改密码")
    @PutMapping("/users/password")
    public Result updatePassword(@Valid @RequestBody UserVO user) {
        userAuthService.updatePassword(user);
        return new Result<>(true, StatusConst.OK, "修改成功！");
    }

    @ApiOperation(value = "修改管理员密码")
    @PutMapping("/admin/users/password")
    public Result updateAdminPassword(@Valid @RequestBody PasswordVO passwordVO) {
        userAuthService.updateAdminPassword(passwordVO);
        return new Result<>(true, StatusConst.OK, "修改成功！");
    }


    @ApiOperation(value = "github登录")
    @ApiImplicitParam(name = "githubCode",value = "githubCode",required = true,dataType = "String")
    @PostMapping("/users/oauth/github")
    public Result<UserInfoDTO> githubLogin(String githubCode) throws IOException {
        UserInfoDTO userinfo = userAuthService.githubLogin(githubCode);
        if (userinfo == null) {
            return new Result<>(false, StatusConst.SYSTEM_ERROR, "登录失败", userinfo);
        } else {
            return new Result<>(true, StatusConst.OK, "登录成功", userinfo);
        }
    }

    @ApiOperation(value = "gitee登录")
    @ApiImplicitParam(name = "giteeCode",value = "giteeCode",required = true,dataType = "String")
    @PostMapping("/users/oauth/gitee")
    public Result<UserInfoDTO> giteeLogin(String giteeCode) throws IOException {
        UserInfoDTO userinfo = userAuthService.giteeLogin(giteeCode);

        if (userinfo == null) {
            return new Result<>(false, StatusConst.SYSTEM_ERROR, "登录失败", null);
        } else {
            return new Result<>(true, StatusConst.OK, "登录成功", userinfo);
        }
    }

}

