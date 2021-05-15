package xyz.zerotower.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import xyz.zerotower.blog.entity.Cover;

/**
 * @author ZeroTower
 * @Entity xyz.zerotower.blog.entity.Cover
 */
@Repository
public interface CoverDao extends BaseMapper<Cover> {

}