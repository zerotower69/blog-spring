package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.dto.UniqueViewDTO;
import xyz.zerotower.blog.entity.UniqueView;


import java.util.List;

/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
public interface UniqueViewService extends IService<UniqueView> {

    /**
     * 统计每日用户量
     */
    void saveUniqueView();

    /**
     * 获取7天用户量统计
     * @return 用户量
     */
    List<UniqueViewDTO> listUniqueViews();

}
