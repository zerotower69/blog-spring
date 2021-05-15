package xyz.zerotower.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author ZeroTower
 * @Date 2021/4/9 21:49
 * @Description
 * @Package xyz.zerotower.blog.dto
 * @PROJECT blog-service
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverDTO {

    private Integer id;

    private String url;

    private String coverUrl;
}
