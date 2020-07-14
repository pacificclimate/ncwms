package uk.ac.rdg.resc.edal.ncwms.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcwmsIndexDatabase implements IndexDatabaseInfo {
    @XmlElement(name = "name")
    private String name = "";

    @XmlElement(name = "result")
    private String result = "";

    NcwmsIndexDatabase() {
    }

    public NcwmsIndexDatabase(String name, String result) {
        super();
        this.name = name;
        this.result = result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getResult() { return result; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ");
        sb.append(name);
        return sb.toString();
    }
}
