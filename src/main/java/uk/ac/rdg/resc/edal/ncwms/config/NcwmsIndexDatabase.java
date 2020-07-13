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

    NcwmsIndexDatabase() {
    }

    public NcwmsIndexDatabase(String name) {
        super();
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ");
        sb.append(name);
        return sb.toString();
    }
}
