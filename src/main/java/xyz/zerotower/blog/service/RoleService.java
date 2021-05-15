package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.dto.RoleDTO;
import xyz.zerotower.blog.dto.UserRoleDTO;
import xyz.zerotower.blog.entity.Role;
import xyz.zerotower.blog.vo.ConditionVO;
import xyz.zerotower.blog.vo.RoleVO;

import java.util.List;

/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
public interface RoleService extends IService<Role> {

    /**
     * 获取用户角色选项
     *
     * @return 角色
     */
    List<UserRoleDTO> listUserRoles();

    /**
     * 查询角色列表
     *
     * @param conditionVO 条件
     * @return 角色列表
     */
    PageDTO<RoleDTO> listRoles(ConditionVO conditionVO);

    /**
     * 保存或更新角色
     *
     * @param roleVO 角色
     */
    void saveOrUpdateRole(RoleVO roleVO);

    /**
     * 删除角色
     * @param roleIdList 角色id列表
     */
    void deleteRoles(List<Integer> roleIdList);

}
