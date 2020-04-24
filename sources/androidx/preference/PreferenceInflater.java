package androidx.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class PreferenceInflater {
    private static final HashMap<String, Constructor> CONSTRUCTOR_MAP = new HashMap<>();
    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = {Context.class, AttributeSet.class};
    private final Object[] mConstructorArgs = new Object[2];
    private final Context mContext;
    private String[] mDefaultPackages;
    private PreferenceManager mPreferenceManager;

    public PreferenceInflater(Context context, PreferenceManager preferenceManager) {
        this.mContext = context;
        init(preferenceManager);
    }

    private void init(PreferenceManager preferenceManager) {
        this.mPreferenceManager = preferenceManager;
        StringBuilder sb = new StringBuilder();
        sb.append(Preference.class.getPackage().getName());
        String str = ".";
        sb.append(str);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(SwitchPreference.class.getPackage().getName());
        sb2.append(str);
        setDefaultPackages(new String[]{sb.toString(), sb2.toString()});
    }

    public void setDefaultPackages(String[] strArr) {
        this.mDefaultPackages = strArr;
    }

    public Context getContext() {
        return this.mContext;
    }

    public Preference inflate(int i, PreferenceGroup preferenceGroup) {
        XmlResourceParser xml = getContext().getResources().getXml(i);
        try {
            return inflate((XmlPullParser) xml, preferenceGroup);
        } finally {
            xml.close();
        }
    }

    public Preference inflate(XmlPullParser xmlPullParser, PreferenceGroup preferenceGroup) {
        int next;
        PreferenceGroup onMergeRoots;
        synchronized (this.mConstructorArgs) {
            AttributeSet asAttributeSet = Xml.asAttributeSet(xmlPullParser);
            this.mConstructorArgs[0] = this.mContext;
            do {
                try {
                    next = xmlPullParser.next();
                    if (next == 2) {
                        break;
                    }
                } catch (InflateException e) {
                    throw e;
                } catch (XmlPullParserException e2) {
                    InflateException inflateException = new InflateException(e2.getMessage());
                    inflateException.initCause(e2);
                    throw inflateException;
                } catch (IOException e3) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(xmlPullParser.getPositionDescription());
                    sb.append(": ");
                    sb.append(e3.getMessage());
                    InflateException inflateException2 = new InflateException(sb.toString());
                    inflateException2.initCause(e3);
                    throw inflateException2;
                }
            } while (next != 1);
            if (next == 2) {
                onMergeRoots = onMergeRoots(preferenceGroup, (PreferenceGroup) createItemFromTag(xmlPullParser.getName(), asAttributeSet));
                rInflate(xmlPullParser, onMergeRoots, asAttributeSet);
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(xmlPullParser.getPositionDescription());
                sb2.append(": No start tag found!");
                throw new InflateException(sb2.toString());
            }
        }
        return onMergeRoots;
    }

    private PreferenceGroup onMergeRoots(PreferenceGroup preferenceGroup, PreferenceGroup preferenceGroup2) {
        if (preferenceGroup != null) {
            return preferenceGroup;
        }
        preferenceGroup2.onAttachedToHierarchy(this.mPreferenceManager);
        return preferenceGroup2;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0078, code lost:
        r10 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0079, code lost:
        r0 = new java.lang.StringBuilder();
        r0.append(r13.getPositionDescription());
        r0.append(r1);
        r0.append(r11);
        r12 = new android.view.InflateException(r0.toString());
        r12.initCause(r10);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0097, code lost:
        throw r12;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0098, code lost:
        r10 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0099, code lost:
        throw r10;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0078 A[ExcHandler: Exception (r10v5 'e' java.lang.Exception A[CUSTOM_DECLARE]), Splitter:B:2:0x000d] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private androidx.preference.Preference createItem(java.lang.String r11, java.lang.String[] r12, android.util.AttributeSet r13) throws java.lang.ClassNotFoundException, android.view.InflateException {
        /*
            r10 = this;
            java.util.HashMap<java.lang.String, java.lang.reflect.Constructor> r0 = CONSTRUCTOR_MAP
            java.lang.Object r0 = r0.get(r11)
            java.lang.reflect.Constructor r0 = (java.lang.reflect.Constructor) r0
            java.lang.String r1 = ": Error inflating class "
            r2 = 1
            if (r0 != 0) goto L_0x006d
            android.content.Context r0 = r10.mContext     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            java.lang.ClassLoader r0 = r0.getClassLoader()     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r3 = 0
            if (r12 == 0) goto L_0x005b
            int r4 = r12.length     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            if (r4 != 0) goto L_0x001a
            goto L_0x005b
        L_0x001a:
            int r4 = r12.length     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r5 = 0
            r6 = r3
            r7 = r5
        L_0x001e:
            if (r6 >= r4) goto L_0x003a
            r8 = r12[r6]     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ ClassNotFoundException -> 0x0036, Exception -> 0x0078 }
            r9.<init>()     // Catch:{ ClassNotFoundException -> 0x0036, Exception -> 0x0078 }
            r9.append(r8)     // Catch:{ ClassNotFoundException -> 0x0036, Exception -> 0x0078 }
            r9.append(r11)     // Catch:{ ClassNotFoundException -> 0x0036, Exception -> 0x0078 }
            java.lang.String r8 = r9.toString()     // Catch:{ ClassNotFoundException -> 0x0036, Exception -> 0x0078 }
            java.lang.Class r5 = java.lang.Class.forName(r8, r3, r0)     // Catch:{ ClassNotFoundException -> 0x0036, Exception -> 0x0078 }
            goto L_0x003a
        L_0x0036:
            r7 = move-exception
            int r6 = r6 + 1
            goto L_0x001e
        L_0x003a:
            if (r5 != 0) goto L_0x005f
            if (r7 != 0) goto L_0x005a
            android.view.InflateException r10 = new android.view.InflateException     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            java.lang.StringBuilder r12 = new java.lang.StringBuilder     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r12.<init>()     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            java.lang.String r0 = r13.getPositionDescription()     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r12.append(r0)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r12.append(r1)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r12.append(r11)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            java.lang.String r12 = r12.toString()     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r10.<init>(r12)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            throw r10     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
        L_0x005a:
            throw r7     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
        L_0x005b:
            java.lang.Class r5 = java.lang.Class.forName(r11, r3, r0)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
        L_0x005f:
            java.lang.Class<?>[] r12 = CONSTRUCTOR_SIGNATURE     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            java.lang.reflect.Constructor r0 = r5.getConstructor(r12)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r0.setAccessible(r2)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            java.util.HashMap<java.lang.String, java.lang.reflect.Constructor> r12 = CONSTRUCTOR_MAP     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r12.put(r11, r0)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
        L_0x006d:
            java.lang.Object[] r10 = r10.mConstructorArgs     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            r10[r2] = r13     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            java.lang.Object r10 = r0.newInstance(r10)     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            androidx.preference.Preference r10 = (androidx.preference.Preference) r10     // Catch:{ ClassNotFoundException -> 0x0098, Exception -> 0x0078 }
            return r10
        L_0x0078:
            r10 = move-exception
            android.view.InflateException r12 = new android.view.InflateException
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r13 = r13.getPositionDescription()
            r0.append(r13)
            r0.append(r1)
            r0.append(r11)
            java.lang.String r11 = r0.toString()
            r12.<init>(r11)
            r12.initCause(r10)
            throw r12
        L_0x0098:
            r10 = move-exception
            throw r10
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.preference.PreferenceInflater.createItem(java.lang.String, java.lang.String[], android.util.AttributeSet):androidx.preference.Preference");
    }

    /* access modifiers changed from: protected */
    public Preference onCreateItem(String str, AttributeSet attributeSet) throws ClassNotFoundException {
        return createItem(str, this.mDefaultPackages, attributeSet);
    }

    private Preference createItemFromTag(String str, AttributeSet attributeSet) {
        try {
            if (-1 == str.indexOf(46)) {
                return onCreateItem(str, attributeSet);
            }
            return createItem(str, null, attributeSet);
        } catch (InflateException e) {
            throw e;
        } catch (ClassNotFoundException e2) {
            StringBuilder sb = new StringBuilder();
            sb.append(attributeSet.getPositionDescription());
            sb.append(": Error inflating class (not found)");
            sb.append(str);
            InflateException inflateException = new InflateException(sb.toString());
            inflateException.initCause(e2);
            throw inflateException;
        } catch (Exception e3) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(attributeSet.getPositionDescription());
            sb2.append(": Error inflating class ");
            sb2.append(str);
            InflateException inflateException2 = new InflateException(sb2.toString());
            inflateException2.initCause(e3);
            throw inflateException2;
        }
    }

    private void rInflate(XmlPullParser xmlPullParser, Preference preference, AttributeSet attributeSet) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if ((next == 3 && xmlPullParser.getDepth() <= depth) || next == 1) {
                return;
            }
            if (next == 2) {
                String name = xmlPullParser.getName();
                String str = "Error parsing preference";
                if ("intent".equals(name)) {
                    try {
                        preference.setIntent(Intent.parseIntent(getContext().getResources(), xmlPullParser, attributeSet));
                    } catch (IOException e) {
                        XmlPullParserException xmlPullParserException = new XmlPullParserException(str);
                        xmlPullParserException.initCause(e);
                        throw xmlPullParserException;
                    }
                } else {
                    String str2 = "extra";
                    if (str2.equals(name)) {
                        getContext().getResources().parseBundleExtra(str2, attributeSet, preference.getExtras());
                        try {
                            skipCurrentTag(xmlPullParser);
                        } catch (IOException e2) {
                            XmlPullParserException xmlPullParserException2 = new XmlPullParserException(str);
                            xmlPullParserException2.initCause(e2);
                            throw xmlPullParserException2;
                        }
                    } else {
                        Preference createItemFromTag = createItemFromTag(name, attributeSet);
                        ((PreferenceGroup) preference).addItemFromInflater(createItemFromTag);
                        rInflate(xmlPullParser, createItemFromTag, attributeSet);
                    }
                }
            }
        }
    }

    private static void skipCurrentTag(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1) {
                return;
            }
            if (next == 3 && xmlPullParser.getDepth() <= depth) {
                return;
            }
        }
    }
}
