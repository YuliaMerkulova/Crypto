package server.cryptoserver.models;

import lombok.AllArgsConstructor;

import java.util.Date;


@AllArgsConstructor
public class Selector {
    public String fileName;
    public String Mode;
    public Float size;
    public Date dateUploding;
    public Integer id;
}
