package xyz.zerotower.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xyz.zerotower.blog.dao.ArticleTagDao;
import xyz.zerotower.blog.entity.ArticleTag;
import xyz.zerotower.blog.service.ArticleTagService;
import org.springframework.stereotype.Service;

/**
 *
 * @author xiaojie
 * @since 2020-05-18
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagDao, ArticleTag> implements ArticleTagService {

}
