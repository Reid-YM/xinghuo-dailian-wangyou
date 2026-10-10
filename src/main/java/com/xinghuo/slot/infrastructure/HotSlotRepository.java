package com.xinghuo.slot.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotSlotRepository extends JpaRepository<HotSlot, Long> {
    Optional<HotSlot> findBySlotNo(int slotNo);

    List<HotSlot> findAllByOrderBySlotNoAsc();
}
