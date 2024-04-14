package org.springframework.bzb.transactionbzb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("dev")
@Service
public class RoleService {

    @Autowired
    private RoleMapper roleMapper;

    public Role getById(int id) {
        return roleMapper.getById(id);
    }

	public int insert(Role role) {
		return roleMapper.insert(role);
	}
}