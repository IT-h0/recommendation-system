package com.recommendation.server.service.impl;

import com.recommendation.common.exception.BusinessException;
import com.recommendation.server.mapper.CartMapper;
import com.recommendation.server.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void deleteCartItem_succeeds_whenBelongsToUser() {
        when(cartMapper.deleteByIdAndUserId(10L, 1L)).thenReturn(1);

        assertThatCode(() -> cartService.deleteCartItem(10L, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteCartItem_throws_whenNotBelongsToUser() {
        // 删除条件带上 user_id，非本人或不存在时返回 0 → 抛业务异常，防止越权
        when(cartMapper.deleteByIdAndUserId(10L, 2L)).thenReturn(0);

        assertThatThrownBy(() -> cartService.deleteCartItem(10L, 2L))
                .isInstanceOf(BusinessException.class);
    }
}
