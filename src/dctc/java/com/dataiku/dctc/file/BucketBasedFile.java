package com.dataiku.dctc.file;

import java.io.IOException;

/**
 * A small abstraction for file systems that have a notion of buckets and paths within a bucket;
 * Each file object has a "state", depending on whether we have stat() this file or not.
 */
public abstract class BucketBasedFile extends AbstractGFile {
    /**
     * Computes the type of this file and relevant info:
     *   - If type is ROOT, should cache the list of buckets
     *   - If type is PATH_IN_BUCKET, should cache the recursive list with metadata info
     *   - If type is FILE, should cache the metadata info for this file
     *
     * Type should not be UNRESOLVED anymore after this method (ie, use finally)
     */
    protected abstract void resolve() throws IOException;

    @Override
    public boolean exists() throws IOException {
        resolve();
        assert(type !=Type.UNRESOLVED);
        assert(type != Type.FAILURE); // If failure, it should have already thrown
        return !(type == Type.NOT_FOUND || type == Type.BUCKET_EXISTS);
    }
    @Override
    public boolean isDirectory() throws IOException {
        if (path == null || path.length() == 0) {
            return true;
        }
        resolve();
        assert(type != Type.FAILURE); // If failure, it should have already thrown
        return type == Type.ROOT || type == Type.DIR;
    }
    @Override
    public boolean isFile() throws IOException {
        resolve();
        assert(type != Type.FAILURE); // If failure, it should have already thrown
        return type == Type.FILE;
    }
    @Override
    public String givenName() {
        return getProtocol() + "://" + FileManipulation.concat(bucket, path, fileSeparator());
    }
    @Override
    public String givenPath() {
        return path;
    }
    @Override
    public String getAbsolutePath() {
        return fileSeparator() + FileManipulation.concat(bucket, path, fileSeparator());
    }
    @Override
    public boolean hasAcl() {
        return true;
    }

    protected enum Type {
        UNRESOLVED,
        FAILURE,
        NOT_FOUND,
        FILE,
        DIR,
        BUCKET_EXISTS, // Is a valid path, and the bucket exists
        ROOT
    };

    protected Type type = Type.UNRESOLVED;
    protected String bucket;
    protected String path;
}
