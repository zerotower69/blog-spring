package xyz.zerotower.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.zerotower.blog.dao.CoverDao;
import xyz.zerotower.blog.dto.CoverDTO;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.entity.Cover;
import xyz.zerotower.blog.service.CoverService;
import xyz.zerotower.blog.vo.CoverVo;

import java.util.List;

/**
 * @Author ZeroTower
 * @Date 2021/4/9 21:47
 * @Description
 * @Package xyz.zerotower.blog.service.impl
 * @PROJECT blog-service
 **/
@Service
public class CoverServiceImpl extends ServiceImpl<CoverDao, Cover>  implements CoverService {

    @Autowired
    protected CoverDao coverDao;


    @Override
    public List<Cover> listCover() {
        return coverDao.selectList(null);
    }

    @Override
    public void deleteCover(List<Integer> coverIdList) {
        coverDao.deleteBatchIds(coverIdList);
    }

    @Override
    public void saveOrUpdateCover(CoverVo coverVo) {
        Cover cover=Cover.builder()
                .id(coverVo.getId())
                .url(coverVo.getUrl())
                .coverUrl(coverVo.getCoverUrl())
                .build();
        this.saveOrUpdate(cover);
    }


}
