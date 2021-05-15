package xyz.zerotower.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import xyz.zerotower.blog.dto.RoleDTO;
import xyz.zerotower.blog.dto.UrlRoleDTO;
import xyz.zerotower.blog.entity.Role;
import xyz.zerotower.blog.vo.ConditionVO;

import java.util.List;

/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
@Repository
public interface RoleDao extends BaseMapper<Role> {

    /**
     * 查询路由角色列表
     *
     * @return 角色标签
     */
    List<UrlRoleDTO> listUrlRoles();

    /**
     * 根据用户id获取角色列表
     *
     * @param userInfoId 用户id
     * @return 角色标签
     */
    List<String> listRolesByUserInfoId(Integer userInfoId);

    /**
     * 查询角色列表
     *
     * @param conditionVO 条件
     * @return 角色列表
     */
    List<RoleDTO> listRoles(@Param("conditionVO") ConditionVO conditionVO);

}
