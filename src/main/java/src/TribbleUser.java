package src;

import sun.util.logging.resources.logging;

import java.util.ArrayDeque;
import java.util.Date;

public class TribbleUser {

    public String userId;
    public Date createTime;
    public ArrayDeque<Long> tribbleDateList;


    public TribbleUser()
    {

    }

    public TribbleUser(String userName, Date createTime)
    {
        this.userId = userName;
        this.createTime = createTime;
        this.tribbleDateList = new ArrayDeque<Long>();
    }

}
