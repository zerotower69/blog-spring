package xyz.zerotower.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import xyz.zerotower.blog.dto.CategoryDTO;
import xyz.zerotower.blog.entity.Category;

import java.util.List;

/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
@Repository
public interface CategoryDao extends BaseMapper<Category> {

    /**
     * 查询分类和对应文章数量
     * @return 分类集合
     */
    List<CategoryDTO> listCategoryDTO();

}
