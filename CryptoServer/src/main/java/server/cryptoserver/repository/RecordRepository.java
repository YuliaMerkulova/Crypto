package server.cryptoserver.repository;

import org.springframework.data.repository.CrudRepository;
import server.cryptoserver.models.MyRecord;

public interface RecordRepository extends CrudRepository<MyRecord, Integer> {

}
