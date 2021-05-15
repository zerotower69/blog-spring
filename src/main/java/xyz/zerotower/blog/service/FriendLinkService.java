package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.dto.FriendLinkBackDTO;
import xyz.zerotower.blog.dto.FriendLinkDTO;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.entity.FriendLink;
import xyz.zerotower.blog.vo.ConditionVO;
import xyz.zerotower.blog.vo.FriendLinkVO;

import java.util.List;

/**
 * @author xiaojie
 * @since 2020-05-18
 */
public interface FriendLinkService extends IService<FriendLink> {

    /**
     * 查看友链列表
     *
     * @return 友链列表
     */
    List<FriendLinkDTO> listFriendLinks();

    /**
     * 查看后台友链列表
     *
     * @param condition 条件
     * @return 友链列表
     */
    PageDTO<FriendLinkBackDTO> listFriendLinkDTO(ConditionVO condition);

    /**
     * 保存或更新友链
     * @param friendLinkVO 友链
     */
    void saveOrUpdateFriendLink(FriendLinkVO friendLinkVO);

}
