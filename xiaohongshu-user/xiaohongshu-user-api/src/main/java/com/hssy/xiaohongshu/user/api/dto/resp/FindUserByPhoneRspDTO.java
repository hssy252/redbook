package com.hssy.xiaohongshu.user.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserByPhoneRspDTO {

    private Long id;

    private String password;

}