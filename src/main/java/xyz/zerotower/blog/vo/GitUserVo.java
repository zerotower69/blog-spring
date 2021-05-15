package xyz.zerotower.blog.vo;

import lombok.Data;

@Data
public class GitUserVo {

    /**
     * github 或者 gitee
     */
    private String type;
    /**
     * 用户名
     */
    private String name;
    /**
     * 头像地址
     */
    private String avatar;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 主页地址
     */
    private String home;
}
