package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.dto.TagDTO;
import xyz.zerotower.blog.entity.Tag;
import xyz.zerotower.blog.vo.ConditionVO;
import xyz.zerotower.blog.vo.TagVO;

import java.util.List;

/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
public interface TagService extends IService<Tag> {

    /**
     * 查询标签列表
     *
     * @return 标签列表
     */
    PageDTO<TagDTO> listTags();

    /**
     * 查询后台标签
     *
     * @param condition 条件
     * @return 标签列表
     */
    PageDTO<Tag> listTagBackDTO(ConditionVO condition);

    /**
     * 删除标签
     *
     * @param tagIdList 标签id集合
     */
    void deleteTag(List<Integer> tagIdList);

    /**
     * 保存或更新标签
     * @param tagVO 标签
     */
    void saveOrUpdateTag(TagVO tagVO);

}
