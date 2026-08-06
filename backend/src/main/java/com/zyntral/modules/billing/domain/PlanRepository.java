package com.zyntral.modules.billing.domain;

import com.zyntral.common.domain.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, PlanCode> {
}
