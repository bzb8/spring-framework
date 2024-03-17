package org.springframework.bzb.transactionbzb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan("org.springframework.bzb.transactionbzb") // 扫描Mapper接口所在的包
public class DataSourceConfig {

    @Autowired
    private Environment env;

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        //config.setDriverClassName("");
        config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/test_office");
        config.setUsername("root");
        config.setPassword("123456");
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource());
        // 如果有额外的配置，如类型处理器等，可在此设置
        // factoryBean.setTypeAliasesPackage(...);
        // factoryBean.setTypeHandlers(...);
		//factoryBean.setMapperLocations(new ClassPathResource("mapper/**"));
        return factoryBean;
    }

	@Bean
	public DataSourceTransactionManager transactionManager(){
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
		transactionManager.setDataSource(dataSource());
		return  transactionManager;
	}
}