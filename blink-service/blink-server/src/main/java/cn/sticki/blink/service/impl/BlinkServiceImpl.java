package cn.sticki.blink.service.impl;

import cn.sticki.blink.mapper.BlinkGeneralMapper;
import cn.sticki.blink.mapper.BlinkMapper;
import cn.sticki.blink.pojo.Blink;
import cn.sticki.blink.pojo.BlinkGeneral;
import cn.sticki.blink.pojo.SaveBlinkBO;
import cn.sticki.blink.pojo.UpdateBlinkBO;
import cn.sticki.blink.sdk.BlinkEvent;
import cn.sticki.blink.sdk.BlinkMqConstants;
import cn.sticki.blink.service.BlinkService;
import cn.sticki.common.exception.MapperException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

/**
 * @author 阿杆
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BlinkServiceImpl extends ServiceImpl<BlinkMapper, Blink> implements BlinkService {

	@Resource
	private BlinkMapper blinkMapper;

	@Resource
	private BlinkGeneralMapper blinkGeneralMapper;

	@Resource
	private RabbitTemplate rabbitTemplate;

	@Override
	public void create(SaveBlinkBO blinkBO) {
		Blink blink = new Blink();
		BeanUtils.copyProperties(blinkBO, blink);
		blink.setCreateTime(new Timestamp(System.currentTimeMillis()));
		blinkMapper.insert(blink);
		BlinkGeneral general = new BlinkGeneral();
		general.setBlinkId(blink.getId());
		general.setScore(getRating(blink));
		blinkGeneralMapper.insert(general);

		// 发布动态创建事件
		BlinkEvent event = BlinkEvent.ofInsert(blink.getId(), blink.getUserId());
		rabbitTemplate.convertAndSend(BlinkMqConstants.BLINK_TOPIC_EXCHANGE, BlinkMqConstants.BLINK_INSERT_KEY, event);
	}

	@Override
	public void remove(int id, int userId) {
		LambdaQueryWrapper<Blink> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Blink::getId, id).eq(Blink::getUserId, userId);
		if (blinkMapper.delete(wrapper) != 1) {
			throw new MapperException("动态删除失败", "id -> " + id);
		}
		blinkGeneralMapper.deleteById(id);

		// 发布动态删除事件
		BlinkEvent event = BlinkEvent.ofDelete(id, userId);
		rabbitTemplate.convertAndSend(BlinkMqConstants.BLINK_TOPIC_EXCHANGE, BlinkMqConstants.BLINK_DELETE_KEY, event);
	}

	@Override
	public void update(UpdateBlinkBO blinkBO) {
		LambdaUpdateWrapper<Blink> wrapper = new LambdaUpdateWrapper<>();
		wrapper.eq(Blink::getId, blinkBO.getId()).eq(Blink::getUserId, blinkBO.getUserId());
		wrapper.set(Blink::getContent, blinkBO.getContent());
		if (blinkMapper.update(null, wrapper) != 1) {
			throw new MapperException("动态更新失败", blinkBO);
		}
	}

	private double getRating(Blink blink) {
		return blink.getContent().length();
	}

}
