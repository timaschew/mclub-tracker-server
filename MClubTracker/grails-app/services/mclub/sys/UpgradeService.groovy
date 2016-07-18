package mclub.sys

import com.github.davidmoten.geo.GeoHash
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

            if(sql.equals("updateDeviceLocationHash")){
                updateDeviceLocationHash(session);
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

    /*
     * Delete position records with devices not found
     */
    private void cleanupOrphanPositions(org.hibernate.classic.Session session){
        // first retrieve the orphan position IDs
        String sql1 = "select tp.id from tracker_position AS tp LEFT OUTER JOIN tracker_device AS td ON tp.device_id = td.id WHERE td.id is null";
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

    /*
     * Update device location hash according to the latest position data
     */
    private void updateDeviceLocationHash(org.hibernate.classic.Session session){
        String sql1 = "SELECT dev.id,pos.latitude,pos.longitude FROM tracker_device AS dev LEFT OUTER JOIN tracker_position AS pos ON dev.latest_position_id=pos.id";
        Query query1 = session.createSQLQuery(sql1);
        def records = query1.list();
        int count = 0;
        records.each{rec ->
            try{
                def devId = rec[0];
                def lat = rec[1];
                def lon = rec[2];
                if(lat && lon){
                    String geohash = GeoHash.encodeHash(lat,lon);
                    // perform the update
                    String sql2 = "UPDATE tracker_device SET location_hash=:hash WHERE id=:id"
                    Query query2 = session.createSQLQuery(sql2);
                    query2.setParameter("hash",geohash);
                    query2.setParameter("id",devId);
                    query2.executeUpdate();
                    count++;
                    log.info "  device[${devId}] location ${lat}/${lon} hash ${geohash} updated"

                    if(count % 100){
                        session.flush();
                    }
                }
            }catch(Exception e){
                log.error "Error updating device location hash", e
                return;
            }
        }
        log.info("Total ${count} records updated");
    }

    /**
     * Read the patch SQL file
     * @param fileName
     * @return
     */
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