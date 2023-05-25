package server.cryptoserver.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import server.cryptoserver.models.MyRecord;
import server.cryptoserver.models.Selector;

import java.util.List;

public interface RecordRepository extends CrudRepository<MyRecord, Integer> {
    @Query("SELECT NEW server.cryptoserver.models.Selector(r.fileName, r.Mode, r.size, r.dateUploading, r.id) FROM MyRecord r")
    List<Selector> findAllFiles();

}
