package org.springframework.aop;

import lombok.Data;
import org.springframework.stereotype.Service;

@Data
@Service
public class Louzai implements ILouzai {

	@Override
    public void everyDay() {
        System.out.println("睡觉");
    }
}