package server.cryptoserver.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Data
@Table(name = "data")
public class MyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @Lob
    private byte[] file;
    String key_;

    String fileName;
    String IV;
    String Mode;
    public MyRecord(byte[] myFile, String myKey, String fileName_, String IV, String Mode){
        file = myFile;
        key_ = myKey;
        fileName = fileName_;
        this.IV = IV;
        this.Mode = Mode;
    }
}
