/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bin.zip;

import bin.zip.extrafield.CentralDirectoryParsingZipExtraField;
import bin.zip.extrafield.ExtraFieldUtils;
import bin.zip.extrafield.UnparseableExtraFieldData;
import bin.zip.extrafield.ZipExtraField;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipException;

/**
 * Extension that adds better handling of extra fields and provides
 * access to the internal and external file attributes.
 * <p>
 * <p>The extra data is expected to follow the recommendation of
 * the .ZIP File Format Specification created by PKWARE Inc. :</p>
 * <ul>
 * <li>the extra byte array consists of a sequence of extra fields</li>
 * <li>each extra fields starts by a two byte header id followed by
 * a two byte sequence holding the length of the remainder of
 * data.</li>
 * </ul>
 * <p>
 * <p>Any extra data that cannot be parsed by the rules above will be
 * consumed as "unparseable" extra data and treated differently by the
 * methods of this class.  Versions prior to Apache Commons Compress
 * 1.1 would have thrown an exception if any attempt was made to read
 * or write extra data not conforming to the recommendation.</p>
 *
 * @see <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">
 * .ZIP File Format Specification</a>
 */
public class ZipEntry extends java.util.zip.ZipEntry implements Cloneable {

    public static final int PLATFORM_UNIX = 3;
    public static final int PLATFORM_FAT = 0;
    private static final int SHORT_MASK = 0xFFFF;
    private static final int SHORT_SHIFT = 16;

    private int internalAttributes = 0;
    private int platform = PLATFORM_FAT;
    private long externalAttributes = 0;
    private LinkedHashMap<ZipShort, ZipExtraField> extraFields = null;
    private UnparseableExtraFieldData unparseableExtra = null;

    private String name = null;
    private String parent = null;
    private String simpleName = null;

    /**
     * Creates a new zip entry with the specified name.
     *
     * @param name the name of the entry
     * @since 1.1
     */
    public ZipEntry(String name) {
        super(name);
        setName(name);
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     *
     * @param entry the entry to get fields from
     * @throws ZipException on error
     * @since 1.1
     */
    public ZipEntry(java.util.zip.ZipEntry entry) throws ZipException {
        super(entry);
        setName(entry.getName());
        byte[] extra = entry.getExtra();
        if (extra != null) {
            setExtraFields(ExtraFieldUtils.parse(extra, true,
                    ExtraFieldUtils
                            .UnparseableExtraField.READ));
        } else {
            // initializes extra data to an empty byte array
            setExtra();
        }
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     *
     * @param entry the entry to get fields from
     * @throws ZipException on error
     * @since 1.1
     */
    public ZipEntry(ZipEntry entry) throws ZipException {
        this((java.util.zip.ZipEntry) entry);
        setInternalAttributes(entry.getInternalAttributes());
        setExternalAttributes(entry.getExternalAttributes());
        setExtraFields(entry.getExtraFields(true));
    }

    /**
     * @since 1.9
     */
    protected ZipEntry() {
        super("");
    }

    /**
     * Overwrite clone.
     *
     * @return a cloned copy of this ZipEntry
     * @since 1.1
     */
    public Object clone() {
        ZipEntry e = (ZipEntry) super.clone();

        e.setInternalAttributes(getInternalAttributes());
        e.setExternalAttributes(getExternalAttributes());
        e.setExtraFields(getExtraFields(true));
        return e;
    }

    /**
     * Retrieves the internal file attributes.
     *
     * @return the internal file attributes
     * @since 1.1
     */
    public int getInternalAttributes() {
        return internalAttributes;
    }

    /**
     * Sets the internal file attributes.
     *
     * @param value an <code>int</code> value
     * @since 1.1
     */
    public void setInternalAttributes(int value) {
        internalAttributes = value;
    }

    /**
     * Retrieves the external file attributes.
     *
     * @return the external file attributes
     * @since 1.1
     */
    public long getExternalAttributes() {
        return externalAttributes;
    }

    /**
     * Sets the external file attributes.
     *
     * @param value an <code>long</code> value
     * @since 1.1
     */
    public void setExternalAttributes(long value) {
        externalAttributes = value;
    }

    /**
     * Sets Unix permissions in a way that is understood by Info-Zip's
     * unzip command.
     *
     * @param mode an <code>int</code> value
     * @since Ant 1.5.2
     */
    @SuppressWarnings("OctalInteger")
    public void setUnixMode(int mode) {
        // CheckStyle:MagicNumberCheck OFF - no point
        setExternalAttributes((mode << SHORT_SHIFT)
                // MS-DOS read-only attribute
                | ((mode & 0200) == 0 ? 1 : 0)
                // MS-DOS directory flag
                | (isDirectory() ? 0x10 : 0));
        // CheckStyle:MagicNumberCheck ON
        platform = PLATFORM_UNIX;
    }

    /**
     * Unix permission.
     *
     * @return the unix permissions
     * @since Ant 1.6
     */
    public int getUnixMode() {
        return platform != PLATFORM_UNIX ? 0 :
                (int) ((getExternalAttributes() >> SHORT_SHIFT) & SHORT_MASK);
    }

    /**
     * Platform specification to put into the &quot;version made
     * by&quot; part of the central file header.
     *
     * @return PLATFORM_FAT unless {@link #setUnixMode setUnixMode}
     * has been called, in which case PLATORM_UNIX will be returned.
     * @since Ant 1.5.2
     */
    public int getPlatform() {
        return platform;
    }

    /**
     * Set the platform (UNIX or FAT).
     *
     * @param platform an <code>int</code> value - 0 is FAT, 3 is UNIX
     * @since 1.9
     */
    protected void setPlatform(int platform) {
        this.platform = platform;
    }

    /**
     * Replaces all currently attached extra fields with the new array.
     *
     * @param fields an array of extra fields
     * @since 1.1
     */
    public void setExtraFields(ZipExtraField[] fields) {
        extraFields = new LinkedHashMap<>();
        for (ZipExtraField field : fields) {
            if (field instanceof UnparseableExtraFieldData) {
                unparseableExtra = (UnparseableExtraFieldData) field;
            } else {
                extraFields.put(field.getHeaderId(), field);
            }
        }
        setExtra();
    }

    /**
     * Retrieves all extra fields that have been parsed successfully.
     *
     * @return an array of the extra fields
     */
    public ZipExtraField[] getExtraFields() {
        return getExtraFields(false);
    }

    /**
     * Retrieves extra fields.
     *
     * @param includeUnparseable whether to also return unparseable
     *                           extra fields as {@link UnparseableExtraFieldData} if such data
     *                           exists.
     * @return an array of the extra fields
     * @since 1.1
     */
    public ZipExtraField[] getExtraFields(boolean includeUnparseable) {
        if (extraFields == null) {
            return !includeUnparseable || unparseableExtra == null
                    ? new ZipExtraField[0]
                    : new ZipExtraField[]{unparseableExtra};
        }
        List<ZipExtraField> result = new ArrayList<>(extraFields.values());
        if (includeUnparseable && unparseableExtra != null) {
            result.add(unparseableExtra);
        }
        return result.toArray(new ZipExtraField[0]);
    }

    /**
     * Adds an extra field - replacing an already present extra field
     * of the same type.
     * <p>
     * <p>If no extra field of the same type exists, the field will be
     * added as last field.</p>
     *
     * @param ze an extra field
     * @since 1.1
     */
    public void addExtraField(ZipExtraField ze) {
        if (ze instanceof UnparseableExtraFieldData) {
            unparseableExtra = (UnparseableExtraFieldData) ze;
        } else {
            if (extraFields == null) {
                extraFields = new LinkedHashMap<>();
            }
            extraFields.put(ze.getHeaderId(), ze);
        }
        setExtra();
    }

    /**
     * Adds an extra field - replacing an already present extra field
     * of the same type.
     * <p>
     * <p>The new extra field will be the first one.</p>
     *
     * @param ze an extra field
     * @since 1.1
     */
    public void addAsFirstExtraField(ZipExtraField ze) {
        if (ze instanceof UnparseableExtraFieldData) {
            unparseableExtra = (UnparseableExtraFieldData) ze;
        } else {
            LinkedHashMap<ZipShort, ZipExtraField> copy = extraFields;
            extraFields = new LinkedHashMap<>();
            extraFields.put(ze.getHeaderId(), ze);
            if (copy != null) {
                copy.remove(ze.getHeaderId());
                extraFields.putAll(copy);
            }
        }
        setExtra();
    }

    /**
     * Remove an extra field.
     *
     * @param type the type of extra field to remove
     * @since 1.1
     */
    public void removeExtraField(ZipShort type) {
        if (extraFields == null) {
            throw new java.util.NoSuchElementException();
        }
        if (extraFields.remove(type) == null) {
            throw new java.util.NoSuchElementException();
        }
        setExtra();
    }

    /**
     * Removes unparseable extra field data.
     */
    public void removeUnparseableExtraFieldData() {
        if (unparseableExtra == null) {
            throw new java.util.NoSuchElementException();
        }
        unparseableExtra = null;
        setExtra();
    }

    /**
     * Looks up an extra field by its header id.
     *
     * @return null if no such field exists.
     */
    public ZipExtraField getExtraField(ZipShort type) {
        if (extraFields != null) {
            return extraFields.get(type);
        }
        return null;
    }

    /**
     * Looks up extra field data that couldn't be parsed correctly.
     *
     * @return null if no such field exists.
     */
    public UnparseableExtraFieldData getUnparseableExtraFieldData() {
        return unparseableExtra;
    }

    /**
     * Parses the given bytes as extra field data and consumes any
     * unparseable data as an {@link UnparseableExtraFieldData}
     * instance.
     *
     * @param extra an array of bytes to be parsed into extra fields
     * @throws RuntimeException if the bytes cannot be parsed
     * @throws RuntimeException on error
     * @since 1.1
     */
    public void setExtra(byte[] extra) throws RuntimeException {
        try {
            ZipExtraField[] local =
                    ExtraFieldUtils.parse(extra, true,
                            ExtraFieldUtils.UnparseableExtraField.READ);
            mergeExtraFields(local, true);
        } catch (Exception e) {
            // actually this is not be possible as of Ant 1.8.1
            throw new RuntimeException("Error parsing extra fields for entry: "
                    + getName() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Unfortunately {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} seems to access the extra data
     * directly, so overriding getExtra doesn't help - we need to
     * modify super's data directly.
     *
     * @since 1.1
     */
    protected void setExtra() {
        super.setExtra(ExtraFieldUtils.mergeLocalFileDataData(getExtraFields(true)));
    }

    /**
     * Sets the central directory part of extra fields.
     */
    public void setCentralDirectoryExtra(byte[] b) {
        try {
            ZipExtraField[] central =
                    ExtraFieldUtils.parse(b, false,
                            ExtraFieldUtils.UnparseableExtraField.READ);
            mergeExtraFields(central, false);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves the extra data for the local file data.
     *
     * @return the extra data for local file
     * @since 1.1
     */
    public byte[] getLocalFileDataExtra() {
        byte[] extra = getExtra();
        return extra != null ? extra : new byte[0];
    }

    public Date getLastModifiedDate() {
        return new Date(getTime());
    }

    /**
     * Retrieves the extra data for the central directory.
     *
     * @return the central directory extra data
     * @since 1.1
     */
    public byte[] getCentralDirectoryExtra() {
        return ExtraFieldUtils.mergeCentralDirectoryData(getExtraFields(true));
    }

    /**
     * Make this class work in JDK 1.1 like a 1.2 class.
     * <p>
     * <p>This either stores the size for later usage or invokes
     * setCompressedSize via reflection.</p>
     *
     * @param size the size to use
     * @since 1.2
     * @deprecated since 1.7.
     * Use setCompressedSize directly.
     */
    public void setComprSize(long size) {
        setCompressedSize(size);
    }

    /**
     * Get the name of the entry.
     *
     * @return the entry name
     * @since 1.9
     */
    public String getName() {
        return name == null ? super.getName() : name;
    }

    public String getParent() {
        return parent;
    }

    public String getSimpleName() {
        return simpleName;
    }

    /**
     * Is this entry a directory?
     *
     * @return true if the entry is a directory
     * @since 1.10
     */
    public boolean isDirectory() {
        return getName().endsWith("/");
    }

    /**
     * Set the name of the entry.
     *
     * @param name the name to use
     */
    public void setName(String name) {
        if (name != null && getPlatform() == PLATFORM_FAT
                && !name.contains("/")) {
            name = name.replace('\\', '/');
        }
        this.name = name;
        setParentAndSimpleName();
    }

    private void setParentAndSimpleName() {
        if (name == null || name.length() == 0)
            return;
        if (name.charAt(name.length() - 1) == '/') {
            int index = name.lastIndexOf('/', name.length() - 2);
            if (index == -1) {
                simpleName = name.substring(0, name.length() - 1);
                parent = null;
            } else {
                simpleName = name.substring(index + 1, name.length() - 1);
                parent = name.substring(0, index + 1);
            }
        } else {
            int index = name.lastIndexOf('/');
            if (index == -1) {
                simpleName = name;
                parent = null;
            } else {
                simpleName = name.substring(index + 1);
                parent = name.substring(0, index + 1);
            }
        }
    }

    private int hash = 0;
    /**
     * Get the hashCode of the entry.
     * This uses the name as the hashcode.
     *
     * @return a hashcode.
     * @since Ant 1.7
     */
    public int hashCode() {
        // this method has severe consequences on performance. We cannot rely
        // on the super.hashCode() method since super.getName() always return
        // the empty string in the current implemention (there's no setter)
        // so it is basically draining the performance of a hashmap lookup
        int h = hash;
        if (h == 0) {
            h = getName().hashCode();
            hash = h;
        }
        return h;
    }

    /**
     * The equality method. In this case, the implementation returns 'this == o'
     * which is basically the equals method of the Object class.
     *
     * @param obj the object to compare to
     * @return true if this object is the same as <code>o</code>
     * @since Ant 1.7
     */
    public boolean equals(Object obj) {
        return this == obj;
    }

    /**
     * If there are no extra fields, use the given fields as new extra
     * data - otherwise merge the fields assuming the existing fields
     * and the new fields stem from different locations inside the
     * archive.
     *
     * @param f     the extra fields to merge
     * @param local whether the new fields originate from local data
     */
    private void mergeExtraFields(ZipExtraField[] f, boolean local)
            throws ZipException {
        if (extraFields == null) {
            setExtraFields(f);
        } else {
            for (ZipExtraField aF : f) {
                ZipExtraField existing;
                if (aF instanceof UnparseableExtraFieldData) {
                    existing = unparseableExtra;
                } else {
                    existing = getExtraField(aF.getHeaderId());
                }
                if (existing == null) {
                    addExtraField(aF);
                } else {
                    if (local
                            || !(existing
                            instanceof CentralDirectoryParsingZipExtraField)) {
                        byte[] b = aF.getLocalFileDataData();
                        existing.parseFromLocalFileData(b, 0, b.length);
                    } else {
                        byte[] b = aF.getCentralDirectoryData();
                        ((CentralDirectoryParsingZipExtraField) existing)
                                .parseFromCentralDirectoryData(b, 0, b.length);
                    }
                }
            }
            setExtra();
        }
    }
}
