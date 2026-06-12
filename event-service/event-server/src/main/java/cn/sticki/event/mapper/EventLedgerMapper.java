package cn.sticki.event.mapper;

import cn.sticki.event.entity.EventLedgerEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EventLedgerMapper extends BaseMapper<EventLedgerEntry> {

	@Select("SELECT * FROM event_ledger WHERE related_user_ids LIKE CONCAT('%', #{userId}, '%') " +
			"AND id > #{afterId} ORDER BY timestamp ASC LIMIT #{limit}")
	List<EventLedgerEntry> selectByUserIdAfter(@Param("userId") int userId,
	                                           @Param("afterId") long afterId,
	                                           @Param("limit") int limit);
}
