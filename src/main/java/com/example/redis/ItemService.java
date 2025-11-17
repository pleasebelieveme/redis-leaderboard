package com.example.redis;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.example.redis.domain.Item;
import com.example.redis.domain.ItemDto;
import com.example.redis.domain.ItemOrder;
import com.example.redis.repository.ItemRepository;
import com.example.redis.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
	private final ZSetOperations<String, ItemDto> rankOps;

    public ItemService(
            ItemRepository itemRepository,
            OrderRepository orderRepository,
			RedisTemplate<String, ItemDto> rankTempalte
    ) {
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
		this.rankOps = rankTempalte.opsForZSet(); // sorted set을 사용하기 위한 메서드
    }

    public void purchase(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        orderRepository.save(ItemOrder.builder()
                .item(item)
                .count(1)
                .build());
		rankOps.incrementScore(
			"soldRanks",
			ItemDto.fromEntity(item),
			1
		); // 없으면 자동으로 만들어서 1증가
    }

	public List<ItemDto> getMostsold() {
		Set<ItemDto> ranks = rankOps.reverseRange("soldRanks", 0, 9); // Rinked Hash Set을 반환(순서보장)
		// RedisTemplate는 null이 가능하기 때문에 코틀린에서 사용하기 어렵다
		if(ranks == null) {
			return Collections.emptyList();
		}
		return ranks.stream().toList();
	}
}
