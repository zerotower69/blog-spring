package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.dto.CoverDTO;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.entity.Cover;
import xyz.zerotower.blog.vo.CoverVo;

import java.util.List;

/**
 * @Author ZeroTower
 * @Date 2021/4/9 21:46
 * @Description
 * @Package xyz.zerotower.blog.service
 * @PROJECT blog-service
 **/
public interface CoverService extends IService<Cover> {

    /**
     * 列出所有的
     * @return
     */
    List<Cover> listCover();

    /**
     * 后台删除 封面
     * @param coverIdList
     */
    void deleteCover(List<Integer> coverIdList);

    /**
     * 保存或者删除
     * @param coverVo
     */
    void saveOrUpdateCover(CoverVo coverVo);


}
