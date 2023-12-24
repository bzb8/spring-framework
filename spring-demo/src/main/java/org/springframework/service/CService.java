package org.springframework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CService {

	private final DService dService;

	private AService aService;

	@Autowired
	public CService(DService dService) {
		this.dService = dService;
		System.out.println(dService);
	}

	public AService getaService() {
		return aService;
	}

	@Autowired
	public void setAService(AService aService) {
		this.aService = aService;
	}
}
