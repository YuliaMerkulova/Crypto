package server.cryptoserver.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.Query;


import java.util.Date;

@NoArgsConstructor
@Entity
@Data
@Table(name = "data")
public class MyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
//    @Lob
//    private byte[] file;
    private int salt;
    String key_;

    String fileName;
    String IV;
    String Mode;

    Float size;
    private Date dateUploading;
    public MyRecord(int mySalt, Float size,  String myKey, String fileName, String IV, String Mode){
        salt = mySalt;
        key_ = myKey;
        this.fileName = fileName;
        this.IV = IV;
        this.Mode = Mode;
        this.size = size;
        dateUploading = new Date();
    }

}
