package mclub.sys

import grails.transaction.Transactional
import mclub.tracker.TrackerPosition
import org.hibernate.Query
import org.hibernate.SessionFactory

/**
 * A simple upgrade service
 */
class UpgradeService {
    SessionFactory sessionFactory;
    ConfigService configService;

    private static final String UPGRADE_SQL_FILE = "/tmp/mtracker_upgrade.sql";

    @Transactional
    def performUpgrade() {
        def sqls = readPatchSQLFile(UPGRADE_SQL_FILE);

        org.hibernate.classic.Session session = sessionFactory.currentSession;
        for(int i = 0;i < sqls.size();i++){
            String sql = sqls[i];

            // special case
            if(sql.equals("cleanupOrphanPositions")){
                cleanupOrphanPositions(session);
                continue;
            }

            // others
            Query query = session.createSQLQuery(sql);
            int rcount = 0;
            try{
                rcount = query.executeUpdate()
                log.info("Execute upgrade SQL ${i}: ${rcount} rows effected");
            }catch(Exception e){
                log.info("Execute upgrade SQL error: ${e.message}, SQL: ${sql}");
            }
        }

        // rename the sql file when all task done without any error
    }

    private void cleanupOrphanPositions(org.hibernate.classic.Session session){
        // first retrieve the orphan position IDs
        String sql1 = "select tp.id from TRACKER_POSITION AS tp LEFT OUTER JOIN TRACKER_DEVICE AS td ON tp.device_id = td.id WHERE td.id is null";
        Query query = session.createSQLQuery(sql1);
        def orphanIDs = query.list();
        int count = 0;
        orphanIDs.each{ recordId ->
            Long id = Long.parseLong(recordId.toString());
            TrackerPosition.executeUpdate("DELETE FROM TrackerPosition tp WHERE tp.id=:id",[id:id]);
            count++;
        }
        log.info("Deleted ${count} orphan position records");
    }

    private Collection<String> readPatchSQLFile(String fileName){
        def sqls = [];
        File upgradeFile = new File(fileName);
        if(upgradeFile.exists()){
            log.info("Upgrading database with ${fileName}");
            upgradeFile.eachLine { aline ->
                String sql = aline.trim();
                if((sql.length() > 0) && !(sql.startsWith('#'))){
                    sqls.add(sql);
                }
            };

            // rename the sql file to .done
            upgradeFile.renameTo(new File("${fileName}.done"));
        }
        return sqls;
    }
}