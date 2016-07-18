dataSource {
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache = false
    cache.use_query_cache = false
	cache.region.factory_class = 'org.hibernate.cache.SingletonEhCacheRegionFactory'
}

// environment specific settings
environments {
    development {
        dataSource {
            //dbCreate = "create" // one of 'create', 'create-drop', 'update', 'validate', ''
			//url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000"

			dbCreate = "update"

			url = "jdbc:h2:/Users/shawn/works/mClub/mclub-tracker-server.git/data/main_data;MVCC=TRUE;LOCK_TIMEOUT=10000"
            pooled=false

            /*
            // for mysql
            pooled=true
            driverClassName="com.mysql.jdbc.Driver"
            dialect="org.hibernate.dialect.MySQL5InnoDBDialect"
            url="jdbc:mysql://localhost:3306/mclub?useUnicode=yes&characterEncoding=UTF-8"
            username="mclub"
            password="secret"
			logSql = true
			*/
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
        }
    }
    production {
        dataSource {
            dbCreate = "update"
			//url = "jdbc:mysql://localhost/mclub-tracker?useUnicode=yes&characterEncoding=UTF-8"
            url = "jdbc:h2:~/mclub-tracker/main_data;MVCC=TRUE;LOCK_TIMEOUT=10000"
            pooled = true
            properties {
               maxActive = -1
               minEvictableIdleTimeMillis=1800000
               timeBetweenEvictionRunsMillis=1800000
               numTestsPerEvictionRun=3
               testOnBorrow=true
               testWhileIdle=true
               testOnReturn=true
               validationQuery="SELECT 1"
            }
        }
    }
}
