package org.redlance.common.utils.requester.boosty.obj;

public class PostSale {
    public int amount;
    public int createdAt;
    public int bloggerId;
    public Post post;
    public BoostyUser user;

    public static class Post {
        public String id;
        public String title;
    }
}
