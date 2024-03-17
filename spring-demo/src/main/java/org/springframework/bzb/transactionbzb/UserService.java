package org.springframework.bzb.transactionbzb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("bzbUserService")
public class UserService {

    @Autowired
    private UserMapper userMapper;

	@Autowired
	private RoleService roleService;

    public User getUserById(int id) {
        return userMapper.getUserById(id);
    }

	@Transactional
	public void insertRole(Role role) {
		int insert = roleService.insert(role);
		System.out.println(insert);
		if (role.getId() == 2) {
			throw new RuntimeException();
		}
	}

}