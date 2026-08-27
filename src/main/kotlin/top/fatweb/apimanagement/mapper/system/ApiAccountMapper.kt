package top.fatweb.apimanagement.mapper.system

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import top.fatweb.apimanagement.entity.system.ApiAccount
import java.math.BigDecimal

/**
 * API account mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see ApiAccount
 */
@Mapper
interface ApiAccountMapper : BaseMapper<ApiAccount> {
    /**
     * Deduct balance atomically, return rows affected
     *
     * @param id Account ID
     * @param cost Cost to deduct
     * @return Rows affected (0 = insufficient balance)
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    fun deduct(@Param("id") id: Long, @Param("cost") cost: BigDecimal): Int

    /**
     * Top up balance atomically
     *
     * @param id Account ID
     * @param amount Amount to top up
     * @return Rows affected
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see BigDecimal
     */
    fun topUp(@Param("id") id: Long, @Param("amount") amount: BigDecimal): Int
}
