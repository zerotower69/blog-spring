package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.dto.MenuDTO;
import xyz.zerotower.blog.dto.UserMenuDTO;
import xyz.zerotower.blog.dto.labelOptionDTO;
import xyz.zerotower.blog.entity.Menu;
import xyz.zerotower.blog.vo.ConditionVO;

import java.util.List;

/**
 * @author: yezhiqiu
 * @date: 2021-01-23
 **/
public interface MenuService extends IService<Menu> {

    /**
     * 查看菜单列表
     * @param conditionVO 条件
     * @return 菜单列表
     */
    List<MenuDTO> listMenus(ConditionVO conditionVO);

    /**
     * 查看角色菜单选项
     * @return 角色菜单选项
     */
    List<labelOptionDTO> listMenuOptions();

    /**
     * 查看用户菜单
     * @return 菜单列表
     */
    List<UserMenuDTO> listUserMenus();

}
