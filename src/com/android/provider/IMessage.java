package com.android.provider;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Build;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Patterns;

import com.android.internal.telephony.SmsApplication;

/**
 * The Telephony provider contains data related to phone operation, specifically SMS and MMS messages and access to the APN list, including the MMSC to use.
 *
 * <p class="note"><strong>Note:</strong> These APIs are not available on all Android-powered devices. If your app depends on telephony features such as for managing SMS messages, include a <a
 * href="{@docRoot} guide/topics/manifest/uses-feature-element.html">{@code &lt;uses-feature>} </a> element in your manifest that declares the {@code "android.hardware.telephony"} hardware feature.
 * Alternatively, you can check for telephony availability at runtime using either {@link android.content.pm.PackageManager#hasSystemFeature hasSystemFeature(PackageManager.FEATURE_TELEPHONY)} or
 * {@link android.telephony.TelephonyManager#getPhoneType}.</p>
 *
 * <h3>Creating an SMS app</h3>
 *
 * <p>Only the default SMS app (selected by the user in system settings) is able to write to the SMS Provider (the tables defined within the {@code Telephony} class) and only the default SMS app
 * receives the {@link android.provider.Telephony.Sms.Intents#SMS_DELIVER_ACTION} broadcast when the user receives an SMS or the {@link android.provider.Telephony.Sms.Intents#WAP_PUSH_DELIVER_ACTION}
 * broadcast when the user receives an MMS.</p>
 *
 * <p>Any app that wants to behave as the user's default SMS app must handle the following intents: <ul> <li>In a broadcast receiver, include an intent filter for
 * {@link Sms.Intents#SMS_DELIVER_ACTION} (<code>"android.provider.Telephony.SMS_DELIVER"</code>). The broadcast receiver must also require the {@link android.Manifest.permission#BROADCAST_SMS}
 * permission. <p>This allows your app to directly receive incoming SMS messages.</p></li> <li>In a broadcast receiver, include an intent filter for {@link Sms.Intents#WAP_PUSH_DELIVER_ACTION} (
 * {@code "android.provider.Telephony.WAP_PUSH_DELIVER"}) with the MIME type <code>"application/vnd.wap.mms-message"</code>. The broadcast receiver must also require the
 * {@link android.Manifest.permission#BROADCAST_WAP_PUSH} permission. <p>This allows your app to directly receive incoming MMS messages.</p></li> <li>In your activity that delivers new messages,
 * include an intent filter for {@link android.content.Intent#ACTION_SENDTO} (<code>"android.intent.action.SENDTO" </code>) with schemas, <code>sms:</code>, <code>smsto:</code>, <code>mms:</code>, and
 * <code>mmsto:</code>. <p>This allows your app to receive intents from other apps that want to deliver a message.</p></li> <li>In a service, include an intent filter for
 * {@link android.telephony.TelephonyManager#ACTION_RESPOND_VIA_MESSAGE} (<code>"android.intent.action.RESPOND_VIA_MESSAGE"</code>) with schemas, <code>sms:</code>, <code>smsto:</code>,
 * <code>mms:</code>, and <code>mmsto:</code>. This service must also require the {@link android.Manifest.permission#SEND_RESPOND_VIA_MESSAGE} permission. <p>This allows users to respond to incoming
 * phone calls with an immediate text message using your app.</p></li> </ul>
 *
 * <p>Other apps that are not selected as the default SMS app can only <em>read</em> the SMS Provider, but may also be notified when a new SMS arrives by listening for the
 * {@link Sms.Intents#SMS_RECEIVED_ACTION} broadcast, which is a non-abortable broadcast that may be delivered to multiple apps. This broadcast is intended for apps that&mdash;while not selected as
 * the default SMS app&mdash;need to read special incoming messages such as to perform phone number verification.</p>
 *
 * <p>For more information about building SMS apps, read the blog post, <a href="http://android-developers.blogspot.com/2013/10/getting-your-sms-apps-ready-for-kitkat.html" >Getting Your SMS Apps
 * Ready for KitKat</a>.</p>
 *
 */
public final class IMessage {
    private static final String TAG = "Telephony";

    /**
     * Not instantiable.
     */
    private IMessage() {
    }

    /**
     * Base columns for tables that contain text-based SMSs.
     */
    public interface TextBasedSmsColumns {

        /** Message type: all messages. */
        public static final int MESSAGE_TYPE_ALL = 0;

        /** Message type: inbox. */
        public static final int MESSAGE_TYPE_INBOX = 1;

        /** Message type: sent messages. */
        public static final int MESSAGE_TYPE_SENT = 2;

        /** Message type: drafts. */
        public static final int MESSAGE_TYPE_DRAFT = 3;

        /** Message type: outbox. */
        public static final int MESSAGE_TYPE_OUTBOX = 4;

        /** Message type: failed outgoing message. */
        public static final int MESSAGE_TYPE_FAILED = 5;

        /** Message type: queued to send later. */
        public static final int MESSAGE_TYPE_QUEUED = 6;

        /**
         * The type of message. <P>Type: INTEGER</P>
         */
        public static final String TYPE = "type";

        /**
         * The thread ID of the message. <P>Type: INTEGER</P>
         */
        public static final String THREAD_ID = "thread_id";

        /**
         * The address of the other party. <P>Type: TEXT</P>
         */
        public static final String ADDRESS = "address";

        /**
         * The date the message was received. <P>Type: INTEGER (long)</P>
         */
        public static final String DATE = "date";

        /**
         * The date the message was sent. <P>Type: INTEGER (long)</P>
         */
        public static final String DATE_SENT = "date_sent";

        /**
         * Has the message been read? <P>Type: INTEGER (boolean)</P>
         */
        public static final String READ = "read";

        /**
         * Has the message been seen by the user? The "seen" flag determines whether we need to show a notification. <P>Type: INTEGER (boolean)</P>
         */
        public static final String SEEN = "seen";

        /**
         * {@code TP-Status} value for the message, or -1 if no status has been received. <P>Type: INTEGER</P>
         */
        public static final String STATUS = "status";

        /** TP-Status: no status received. */
        public static final int STATUS_NONE = -1;
        /** TP-Status: complete. */
        public static final int STATUS_COMPLETE = 0;
        /** TP-Status: pending. */
        public static final int STATUS_PENDING = 32;
        /** TP-Status: failed. */
        public static final int STATUS_FAILED = 64;

        /**
         * The subject of the message, if present. <P>Type: TEXT</P>
         */
        public static final String SUBJECT = "subject";

        /**
         * The body of the message. <P>Type: TEXT</P>
         */
        public static final String BODY = "body";

        /**
         * The ID of the sender of the conversation, if present. <P>Type: INTEGER (reference to item in {@code content://contacts/people})</P>
         */
        public static final String PERSON = "person";

        /**
         * The protocol identifier code. <P>Type: INTEGER</P>
         */
        public static final String PROTOCOL = "protocol";

        /**
         * Is the {@code TP-Reply-Path} flag set? <P>Type: BOOLEAN</P>
         */
        public static final String REPLY_PATH_PRESENT = "reply_path_present";

        /**
         * The service center (SC) through which to send the message, if present. <P>Type: TEXT</P>
         */
        public static final String SERVICE_CENTER = "service_center";

        /**
         * Is the message locked? <P>Type: INTEGER (boolean)</P>
         */
        public static final String LOCKED = "locked";

        /**
         * Error code associated with sending or receiving this message. <P>Type: INTEGER</P>
         */
        public static final String ERROR_CODE = "error_code";
    }

    /**
     * Contains all text-based SMS messages.
     */
    public static final class Sms implements BaseColumns, TextBasedSmsColumns {

        /**
         * Not instantiable.
         * 
         * @hide
         */
        private Sms() {
        }

        /**
         * Used to determine the currently configured default SMS package.
         * 
         * @param context context of the requesting application
         * @return package name for the default SMS package or null
         */
        public static String getDefaultSmsPackage(Context context) {
            if (Build.VERSION.SDK_INT < 19) {
                return context.getPackageName();
            }
            ComponentName component = SmsApplication.getDefaultSmsApplication(context, false);
            if (component != null) {
                return component.getPackageName();
            }
            return null;
        }

        /**
         * Return cursor for table query.
         * 
         * @hide
         */
        public static Cursor query(ContentResolver cr, String[] projection) {
            return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
        }

        /**
         * Return cursor for table query.
         * 
         * @hide
         */
        public static Cursor query(ContentResolver cr, String[] projection, String where, String orderBy) {
            return cr.query(CONTENT_URI, projection, where, null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
        }

        /**
         * The {@code content://} style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://sms-yang");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "date DESC";

        /**
         * Add an SMS to the given URI.
         *
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param address the address of the sender
         * @param body the body of the message
         * @param subject the pseudo-subject of the message
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param deliveryReport true if a delivery report was requested, false if not
         * @return the URI for the new message
         * @hide
         */
        public static Uri addMessageToUri(ContentResolver resolver, Uri uri, String address, String body, String subject, Long date, boolean read, boolean deliveryReport) {
            return addMessageToUri(resolver, uri, address, body, subject, date, read, deliveryReport, -1L);
        }

        /**
         * Add an SMS to the given URI with the specified thread ID.
         *
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param address the address of the sender
         * @param body the body of the message
         * @param subject the pseudo-subject of the message
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param deliveryReport true if a delivery report was requested, false if not
         * @param threadId the thread_id of the message
         * @return the URI for the new message
         * @hide
         */
        public static Uri addMessageToUri(ContentResolver resolver, Uri uri, String address, String body, String subject, Long date, boolean read, boolean deliveryReport, long threadId) {
            ContentValues values = new ContentValues(7);

            values.put(ADDRESS, address);
            if (date != null) {
                values.put(DATE, date);
            }
            values.put(READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
            values.put(SUBJECT, subject);
            values.put(BODY, body);
            if (deliveryReport) {
                values.put(STATUS, STATUS_PENDING);
            }
            if (threadId != -1L) {
                values.put(THREAD_ID, threadId);
            }
            return resolver.insert(uri, values);
        }

        /**
         * Move a message to the given folder.
         *
         * @param context the context to use
         * @param uri the message to move
         * @param folder the folder to move to
         * @return true if the operation succeeded
         * @hide
         */
        public static boolean moveMessageToFolder(Context context, Uri uri, int folder, int error) {
            if (uri == null) {
                return false;
            }

            boolean markAsUnread = false;
            boolean markAsRead = false;
            switch (folder) {
            case MESSAGE_TYPE_INBOX:
            case MESSAGE_TYPE_DRAFT:
                break;
            case MESSAGE_TYPE_OUTBOX:
            case MESSAGE_TYPE_SENT:
                markAsRead = true;
                break;
            case MESSAGE_TYPE_FAILED:
            case MESSAGE_TYPE_QUEUED:
                markAsUnread = true;
                break;
            default:
                return false;
            }

            ContentValues values = new ContentValues(3);

            values.put(TYPE, folder);
            if (markAsUnread) {
                values.put(READ, 0);
            } else if (markAsRead) {
                values.put(READ, 1);
            }
            values.put(ERROR_CODE, error);

            return 1 == SqliteWrapper.update(context, context.getContentResolver(), uri, values, null, null);
        }

        /**
         * Returns true iff the folder (message type) identifies an outgoing message.
         * 
         * @hide
         */
        public static boolean isOutgoingFolder(int messageType) {
            return (messageType == MESSAGE_TYPE_FAILED) || (messageType == MESSAGE_TYPE_OUTBOX) || (messageType == MESSAGE_TYPE_SENT) || (messageType == MESSAGE_TYPE_QUEUED);
        }

        /**
         * Contains all text-based SMS messages in the SMS app inbox.
         */
        public static final class Inbox implements BaseColumns, TextBasedSmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Inbox() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://sms-yang/inbox");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";

            /**
             * Add an SMS to the Draft box.
             *
             * @param resolver the content resolver to use
             * @param address the address of the sender
             * @param body the body of the message
             * @param subject the pseudo-subject of the message
             * @param date the timestamp for the message
             * @param read true if the message has been read, false if not
             * @return the URI for the new message
             * @hide
             */
            public static Uri addMessage(ContentResolver resolver, String address, String body, String subject, Long date, boolean read) {
                return addMessageToUri(resolver, CONTENT_URI, address, body, subject, date, read, false);
            }
        }

        /**
         * Contains all sent text-based SMS messages in the SMS app.
         */
        public static final class Sent implements BaseColumns, TextBasedSmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Sent() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://sms-yang/sent");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";

            /**
             * Add an SMS to the Draft box.
             *
             * @param resolver the content resolver to use
             * @param address the address of the sender
             * @param body the body of the message
             * @param subject the pseudo-subject of the message
             * @param date the timestamp for the message
             * @return the URI for the new message
             * @hide
             */
            public static Uri addMessage(ContentResolver resolver, String address, String body, String subject, Long date) {
                return addMessageToUri(resolver, CONTENT_URI, address, body, subject, date, true, false);
            }
        }

        /**
         * Contains all sent text-based SMS messages in the SMS app.
         */
        public static final class Draft implements BaseColumns, TextBasedSmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Draft() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://sms-yang/draft");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";
        }

        /**
         * Contains all pending outgoing text-based SMS messages.
         */
        public static final class Outbox implements BaseColumns, TextBasedSmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Outbox() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://sms-yang/outbox");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";

            /**
             * Add an SMS to the outbox.
             *
             * @param resolver the content resolver to use
             * @param address the address of the sender
             * @param body the body of the message
             * @param subject the pseudo-subject of the message
             * @param date the timestamp for the message
             * @param deliveryReport whether a delivery report was requested for the message
             * @return the URI for the new message
             * @hide
             */
            public static Uri addMessage(ContentResolver resolver, String address, String body, String subject, Long date, boolean deliveryReport, long threadId) {
                return addMessageToUri(resolver, CONTENT_URI, address, body, subject, date, true, deliveryReport, threadId);
            }
        }

        /**
         * Contains all sent text-based SMS messages in the SMS app.
         */
        public static final class Conversations implements BaseColumns, TextBasedSmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Conversations() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://sms-yang/conversations");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";

            /**
             * The first 45 characters of the body of the message. <P>Type: TEXT</P>
             */
            public static final String SNIPPET = "snippet";

            /**
             * The number of messages in the conversation. <P>Type: INTEGER</P>
             */
            public static final String MESSAGE_COUNT = "msg_count";
        }

        /**
         * Contains constants for SMS related Intents that are broadcast.
         */
        public static final class Intents {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Intents() {
            }

            /**
             * Set by BroadcastReceiver to indicate that the message was handled successfully.
             */
            public static final int RESULT_SMS_HANDLED = 1;

            /**
             * Set by BroadcastReceiver to indicate a generic error while processing the message.
             */
            public static final int RESULT_SMS_GENERIC_ERROR = 2;

            /**
             * Set by BroadcastReceiver to indicate insufficient memory to store the message.
             */
            public static final int RESULT_SMS_OUT_OF_MEMORY = 3;

            /**
             * Set by BroadcastReceiver to indicate that the message, while possibly valid, is of a format or encoding that is not supported.
             */
            public static final int RESULT_SMS_UNSUPPORTED = 4;

            /**
             * Set by BroadcastReceiver to indicate a duplicate incoming message.
             */
            public static final int RESULT_SMS_DUPLICATED = 5;

            /**
             * Activity action: Ask the user to change the default SMS application. This will show a dialog that asks the user whether they want to replace the current default SMS application with the
             * one specified in {@link #EXTRA_PACKAGE_NAME}.
             */
            @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
            public static final String ACTION_CHANGE_DEFAULT = "android.provider.Telephony.ACTION_CHANGE_DEFAULT";

            /**
             * The PackageName string passed in as an extra for {@link #ACTION_CHANGE_DEFAULT}
             *
             * @see #ACTION_CHANGE_DEFAULT
             */
            public static final String EXTRA_PACKAGE_NAME = "package";

            /**
             * Broadcast Action: A new text-based SMS message has been received by the device. This intent will only be delivered to the default sms app. That app is responsible for writing the
             * message and notifying the user. The intent will have the following extra values:</p>
             *
             * <ul> <li><em>"pdus"</em> - An Object[] of byte[]s containing the PDUs that make up the message.</li> </ul>
             *
             * <p>The extra values can be extracted using {@link #getMessagesFromIntent(Intent)}.</p>
             *
             * <p>If a BroadcastReceiver encounters an error while processing this intent it should set the result code appropriately.</p>
             *
             * <p class="note"><strong>Note:</strong> The broadcast receiver that filters for this intent must declare {@link android.Manifest.permission#BROADCAST_SMS} as a required permission in the
             * <a href="{@docRoot} guide/topics/manifest/receiver-element.html">{@code &lt;receiver>}</a> tag.
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER";

            /**
             * Broadcast Action: A new text-based SMS message has been received by the device. This intent will be delivered to all registered receivers as a notification. These apps are not expected
             * to write the message or notify the user. The intent will have the following extra values:</p>
             *
             * <ul> <li><em>"pdus"</em> - An Object[] of byte[]s containing the PDUs that make up the message.</li> </ul>
             *
             * <p>The extra values can be extracted using {@link #getMessagesFromIntent(Intent)}.</p>
             *
             * <p>If a BroadcastReceiver encounters an error while processing this intent it should set the result code appropriately.</p>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

            /**
             * Broadcast Action: A new data based SMS message has been received by the device. This intent will be delivered to all registered receivers as a notification. The intent will have the
             * following extra values:</p>
             *
             * <ul> <li><em>"pdus"</em> - An Object[] of byte[]s containing the PDUs that make up the message.</li> </ul>
             *
             * <p>The extra values can be extracted using {@link #getMessagesFromIntent(Intent)}.</p>
             *
             * <p>If a BroadcastReceiver encounters an error while processing this intent it should set the result code appropriately.</p>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String DATA_SMS_RECEIVED_ACTION = "android.intent.action.DATA_SMS_RECEIVED";

            /**
             * Broadcast Action: A new WAP PUSH message has been received by the device. This intent will only be delivered to the default sms app. That app is responsible for writing the message and
             * notifying the user. The intent will have the following extra values:</p>
             *
             * <ul> <li><em>"transactionId"</em> - (Integer) The WAP transaction ID</li> <li><em>"pduType"</em> - (Integer) The WAP PDU type</li> <li><em>"header"</em> - (byte[]) The header of the
             * message</li> <li><em>"data"</em> - (byte[]) The data payload of the message</li> <li><em>"contentTypeParameters" </em> -(HashMap&lt;String,String&gt;) Any parameters associated with the
             * content type (decoded from the WSP Content-Type header)</li> </ul>
             *
             * <p>If a BroadcastReceiver encounters an error while processing this intent it should set the result code appropriately.</p>
             *
             * <p>The contentTypeParameters extra value is map of content parameters keyed by their names.</p>
             *
             * <p>If any unassigned well-known parameters are encountered, the key of the map will be 'unassigned/0x...', where '...' is the hex value of the unassigned parameter. If a parameter has
             * No-Value the value in the map will be null.</p>
             *
             * <p class="note"><strong>Note:</strong> The broadcast receiver that filters for this intent must declare {@link android.Manifest.permission#BROADCAST_WAP_PUSH} as a required permission
             * in the <a href="{@docRoot} guide/topics/manifest/receiver-element.html">{@code &lt;receiver>}</a> tag.
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String WAP_PUSH_DELIVER_ACTION = "android.provider.Telephony.WAP_PUSH_DELIVER";

            /**
             * Broadcast Action: A new WAP PUSH message has been received by the device. This intent will be delivered to all registered receivers as a notification. These apps are not expected to
             * write the message or notify the user. The intent will have the following extra values:</p>
             *
             * <ul> <li><em>"transactionId"</em> - (Integer) The WAP transaction ID</li> <li><em>"pduType"</em> - (Integer) The WAP PDU type</li> <li><em>"header"</em> - (byte[]) The header of the
             * message</li> <li><em>"data"</em> - (byte[]) The data payload of the message</li> <li><em>"contentTypeParameters"</em> - (HashMap&lt;String,String&gt;) Any parameters associated with the
             * content type (decoded from the WSP Content-Type header)</li> </ul>
             *
             * <p>If a BroadcastReceiver encounters an error while processing this intent it should set the result code appropriately.</p>
             *
             * <p>The contentTypeParameters extra value is map of content parameters keyed by their names.</p>
             *
             * <p>If any unassigned well-known parameters are encountered, the key of the map will be 'unassigned/0x...', where '...' is the hex value of the unassigned parameter. If a parameter has
             * No-Value the value in the map will be null.</p>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String WAP_PUSH_RECEIVED_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";

            /**
             * Broadcast Action: A new Cell Broadcast message has been received by the device. The intent will have the following extra values:</p>
             *
             * <ul> <li><em>"message"</em> - An SmsCbMessage object containing the broadcast message data. This is not an emergency alert, so ETWS and CMAS data will be null.</li> </ul>
             *
             * <p>The extra values can be extracted using {@link #getMessagesFromIntent(Intent)}.</p>
             *
             * <p>If a BroadcastReceiver encounters an error while processing this intent it should set the result code appropriately.</p>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String SMS_CB_RECEIVED_ACTION = "android.provider.Telephony.SMS_CB_RECEIVED";

            /**
             * Broadcast Action: A new Emergency Broadcast message has been received by the device. The intent will have the following extra values:</p>
             *
             * <ul> <li><em>"message"</em> - An SmsCbMessage object containing the broadcast message data, including ETWS or CMAS warning notification info if present.</li> </ul>
             *
             * <p>The extra values can be extracted using {@link #getMessagesFromIntent(Intent)}.</p>
             *
             * <p>If a BroadcastReceiver encounters an error while processing this intent it should set the result code appropriately.</p>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String SMS_EMERGENCY_CB_RECEIVED_ACTION = "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED";

            /**
             * Broadcast Action: A new CDMA SMS has been received containing Service Category Program Data (updates the list of enabled broadcast channels). The intent will have the following extra
             * values:</p>
             *
             * <ul> <li><em>"operations"</em> - An array of CdmaSmsCbProgramData objects containing the service category operations (add/delete/clear) to perform.</li> </ul>
             *
             * <p>The extra values can be extracted using {@link #getMessagesFromIntent(Intent)}.</p>
             *
             * <p>If a BroadcastReceiver encounters an error while processing this intent it should set the result code appropriately.</p>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION = "android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED";

            /**
             * Broadcast Action: The SIM storage for SMS messages is full. If space is not freed, messages targeted for the SIM (class 2) may not be saved.
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String SIM_FULL_ACTION = "android.provider.Telephony.SIM_FULL";

            /**
             * Broadcast Action: An incoming SMS has been rejected by the telephony framework. This intent is sent in lieu of any of the RECEIVED_ACTION intents. The intent will have the following
             * extra value:</p>
             *
             * <ul> <li><em>"result"</em> - An int result code, e.g. {@link #RESULT_SMS_OUT_OF_MEMORY} indicating the error returned to the network.</li> </ul>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String SMS_REJECTED_ACTION = "android.provider.Telephony.SMS_REJECTED";

            /**
             * Read the PDUs out of an {@link #SMS_RECEIVED_ACTION} or a {@link #DATA_SMS_RECEIVED_ACTION} intent.
             *
             * @param intent the intent to read from
             * @return an array of SmsMessages for the PDUs
             */
            public static SmsMessage[] getMessagesFromIntent(Intent intent) {
                Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
                String format = intent.getStringExtra("format");

                int pduCount = messages.length;
                SmsMessage[] msgs = new SmsMessage[pduCount];

                for (int i = 0; i < pduCount; i++) {
                    byte[] pdu = (byte[]) messages[i];
                    msgs[i] = SmsMessage.createFromPdu(pdu, format);
                }
                return msgs;
            }
        }
    }

    /**
     * Base columns for tables that contain MMSs.
     */
    public interface BaseMmsColumns extends BaseColumns {

        /** Message box: all messages. */
        public static final int MESSAGE_BOX_ALL = 0;
        /** Message box: inbox. */
        public static final int MESSAGE_BOX_INBOX = 1;
        /** Message box: sent messages. */
        public static final int MESSAGE_BOX_SENT = 2;
        /** Message box: drafts. */
        public static final int MESSAGE_BOX_DRAFTS = 3;
        /** Message box: outbox. */
        public static final int MESSAGE_BOX_OUTBOX = 4;

        /**
         * The thread ID of the message. <P>Type: INTEGER (long)</P>
         */
        public static final String THREAD_ID = "thread_id";

        /**
         * The date the message was received. <P>Type: INTEGER (long)</P>
         */
        public static final String DATE = "date";

        /**
         * The date the message was sent. <P>Type: INTEGER (long)</P>
         */
        public static final String DATE_SENT = "date_sent";

        /**
         * The box which the message belongs to, e.g. {@link #MESSAGE_BOX_INBOX}. <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_BOX = "msg_box";

        /**
         * Has the message been read? <P>Type: INTEGER (boolean)</P>
         */
        public static final String READ = "read";

        /**
         * Has the message been seen by the user? The "seen" flag determines whether we need to show a new message notification. <P>Type: INTEGER (boolean)</P>
         */
        public static final String SEEN = "seen";

        /**
         * Does the message have only a text part (can also have a subject) with no picture, slideshow, sound, etc. parts? <P>Type: INTEGER (boolean)</P>
         */
        public static final String TEXT_ONLY = "text_only";

        /**
         * The {@code Message-ID} of the message. <P>Type: TEXT</P>
         */
        public static final String MESSAGE_ID = "m_id";

        /**
         * The subject of the message, if present. <P>Type: TEXT</P>
         */
        public static final String SUBJECT = "sub";

        /**
         * The character set of the subject, if present. <P>Type: INTEGER</P>
         */
        public static final String SUBJECT_CHARSET = "sub_cs";

        /**
         * The {@code Content-Type} of the message. <P>Type: TEXT</P>
         */
        public static final String CONTENT_TYPE = "ct_t";

        /**
         * The {@code Content-Location} of the message. <P>Type: TEXT</P>
         */
        public static final String CONTENT_LOCATION = "ct_l";

        /**
         * The expiry time of the message. <P>Type: INTEGER (long)</P>
         */
        public static final String EXPIRY = "exp";

        /**
         * The class of the message. <P>Type: TEXT</P>
         */
        public static final String MESSAGE_CLASS = "m_cls";

        /**
         * The type of the message defined by MMS spec. <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_TYPE = "m_type";

        /**
         * The version of the specification that this message conforms to. <P>Type: INTEGER</P>
         */
        public static final String MMS_VERSION = "v";

        /**
         * The size of the message. <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_SIZE = "m_size";

        /**
         * The priority of the message. <P>Type: INTEGER</P>
         */
        public static final String PRIORITY = "pri";

        /**
         * The {@code read-report} of the message. <P>Type: INTEGER (boolean)</P>
         */
        public static final String READ_REPORT = "rr";

        /**
         * Is read report allowed? <P>Type: INTEGER (boolean)</P>
         */
        public static final String REPORT_ALLOWED = "rpt_a";

        /**
         * The {@code response-status} of the message. <P>Type: INTEGER</P>
         */
        public static final String RESPONSE_STATUS = "resp_st";

        /**
         * The {@code status} of the message. <P>Type: INTEGER</P>
         */
        public static final String STATUS = "st";

        /**
         * The {@code transaction-id} of the message. <P>Type: TEXT</P>
         */
        public static final String TRANSACTION_ID = "tr_id";

        /**
         * The {@code retrieve-status} of the message. <P>Type: INTEGER</P>
         */
        public static final String RETRIEVE_STATUS = "retr_st";

        /**
         * The {@code retrieve-text} of the message. <P>Type: TEXT</P>
         */
        public static final String RETRIEVE_TEXT = "retr_txt";

        /**
         * The character set of the retrieve-text. <P>Type: INTEGER</P>
         */
        public static final String RETRIEVE_TEXT_CHARSET = "retr_txt_cs";

        /**
         * The {@code read-status} of the message. <P>Type: INTEGER</P>
         */
        public static final String READ_STATUS = "read_status";

        /**
         * The {@code content-class} of the message. <P>Type: INTEGER</P>
         */
        public static final String CONTENT_CLASS = "ct_cls";

        /**
         * The {@code delivery-report} of the message. <P>Type: INTEGER</P>
         */
        public static final String DELIVERY_REPORT = "d_rpt";

        /**
         * The {@code delivery-time} of the message. <P>Type: INTEGER</P>
         */
        public static final String DELIVERY_TIME = "d_tm";

        /**
         * The {@code response-text} of the message. <P>Type: TEXT</P>
         */
        public static final String RESPONSE_TEXT = "resp_txt";

        /**
         * Is the message locked? <P>Type: INTEGER (boolean)</P>
         */
        public static final String LOCKED = "locked";
    }

    /**
     * Columns for the "canonical_addresses" table used by MMS and SMS.
     */
    public interface CanonicalAddressesColumns extends BaseColumns {
        /**
         * An address used in MMS or SMS. Email addresses are converted to lower case and are compared by string equality. Other addresses are compared using PHONE_NUMBERS_EQUAL. <P>Type: TEXT</P>
         */
        public static final String ADDRESS = "address";
    }

    /**
     * Columns for the "threads" table used by MMS and SMS.
     */
    public interface ThreadsColumns extends BaseColumns {

        /**
         * The date at which the thread was created. <P>Type: INTEGER (long)</P>
         */
        public static final String DATE = "date";

        /**
         * A string encoding of the recipient IDs of the recipients of the message, in numerical order and separated by spaces. <P>Type: TEXT</P>
         */
        public static final String RECIPIENT_IDS = "recipient_ids";

        /**
         * The message count of the thread. <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_COUNT = "message_count";

        /**
         * Indicates whether all messages of the thread have been read. <P>Type: INTEGER</P>
         */
        public static final String READ = "read";

        /**
         * The snippet of the latest message in the thread. <P>Type: TEXT</P>
         */
        public static final String SNIPPET = "snippet";

        /**
         * The charset of the snippet. <P>Type: INTEGER</P>
         */
        public static final String SNIPPET_CHARSET = "snippet_cs";

        /**
         * Type of the thread, either {@link Threads#COMMON_THREAD} or {@link Threads#BROADCAST_THREAD}. <P>Type: INTEGER</P>
         */
        public static final String TYPE = "type";

        /**
         * Indicates whether there is a transmission error in the thread. <P>Type: INTEGER</P>
         */
        public static final String ERROR = "error";

        /**
         * Indicates whether this thread contains any attachments. <P>Type: INTEGER</P>
         */
        public static final String HAS_ATTACHMENT = "has_attachment";
    }

    /**
     * Helper functions for the "threads" table used by MMS and SMS.
     */
    public static final class Threads implements ThreadsColumns {

        private static final String[] ID_PROJECTION = { BaseColumns._ID };

        /**
         * Private {@code content://} style URL for this table. Used by {@link #getOrCreateThreadId(android.content.Context, java.util.Set)}.
         */
        private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms-yang/threadID");

        /**
         * The {@code content://} style URL for this table, by conversation.
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(MmsSms.CONTENT_URI, "conversations");

        /**
         * The {@code content://} style URL for this table, for obsolete threads.
         */
        public static final Uri OBSOLETE_THREADS_URI = Uri.withAppendedPath(CONTENT_URI, "obsolete");

        /** Thread type: common thread. */
        public static final int COMMON_THREAD = 0;

        /** Thread type: broadcast thread. */
        public static final int BROADCAST_THREAD = 1;

        /**
         * Not instantiable.
         * 
         * @hide
         */
        private Threads() {
        }

        /**
         * This is a single-recipient version of {@code getOrCreateThreadId}. It's convenient for use with SMS messages.
         * 
         * @param context the context object to use.
         * @param recipient the recipient to send to.
         * @hide
         */
        public static long getOrCreateThreadId(Context context, String recipient) {
            Set<String> recipients = new HashSet<String>();

            recipients.add(recipient);
            return getOrCreateThreadId(context, recipients);
        }

        /**
         * Given the recipients list and subject of an unsaved message, return its thread ID. If the message starts a new thread, allocate a new thread ID. Otherwise, use the appropriate existing
         * thread ID.
         *
         * <p>Find the thread ID of the same set of recipients (in any order, without any additions). If one is found, return it. Otherwise, return a unique thread ID.</p>
         * 
         * @hide
         */
        public static long getOrCreateThreadId(Context context, Set<String> recipients) {
            Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();

            for (String recipient : recipients) {
                if (Mms.isEmailAddress(recipient)) {
                    recipient = Mms.extractAddrSpec(recipient);
                }

                uriBuilder.appendQueryParameter("recipient", recipient);
            }

            Uri uri = uriBuilder.build();
            //if (DEBUG) Rlog.v(TAG, "getOrCreateThreadId uri: " + uri);

            Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), uri, ID_PROJECTION, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0);
                    } else {
                        Rlog.e(TAG, "getOrCreateThreadId returned no rows!");
                    }
                } finally {
                    cursor.close();
                }
            }

            Rlog.e(TAG, "getOrCreateThreadId failed with uri " + uri.toString());
            throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
        }
    }

    /**
     * Contains all MMS messages.
     */
    public static final class Mms implements BaseMmsColumns {

        /**
         * Not instantiable.
         * 
         * @hide
         */
        private Mms() {
        }

        /**
         * The {@code content://} URI for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://mms-yang");

        /**
         * Content URI for getting MMS report requests.
         */
        public static final Uri REPORT_REQUEST_URI = Uri.withAppendedPath(CONTENT_URI, "report-request");

        /**
         * Content URI for getting MMS report status.
         */
        public static final Uri REPORT_STATUS_URI = Uri.withAppendedPath(CONTENT_URI, "report-status");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "date DESC";

        /**
         * Regex pattern for names and email addresses. <ul> <li><em>mailbox</em> = {@code name-addr}</li> <li><em>name-addr</em> = {@code [display-name] angle-addr}</li> <li><em>angle-addr</em> =
         * {@code [CFWS] "<" addr-spec ">" [CFWS]}</li> </ul>
         * 
         * @hide
         */
        public static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

        /**
         * Helper method to query this table.
         * 
         * @hide
         */
        public static Cursor query(ContentResolver cr, String[] projection) {
            return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
        }

        /**
         * Helper method to query this table.
         * 
         * @hide
         */
        public static Cursor query(ContentResolver cr, String[] projection, String where, String orderBy) {
            return cr.query(CONTENT_URI, projection, where, null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
        }

        /**
         * Helper method to extract email address from address string.
         * 
         * @hide
         */
        public static String extractAddrSpec(String address) {
            Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

            if (match.matches()) {
                return match.group(2);
            }
            return address;
        }

        /**
         * Is the specified address an email address?
         *
         * @param address the input address to test
         * @return true if address is an email address; false otherwise.
         * @hide
         */
        public static boolean isEmailAddress(String address) {
            if (TextUtils.isEmpty(address)) {
                return false;
            }

            String s = extractAddrSpec(address);
            Matcher match = Patterns.EMAIL_ADDRESS.matcher(s);
            return match.matches();
        }

        /**
         * Is the specified number a phone number?
         *
         * @param number the input number to test
         * @return true if number is a phone number; false otherwise.
         * @hide
         */
        public static boolean isPhoneNumber(String number) {
            if (TextUtils.isEmpty(number)) {
                return false;
            }

            Matcher match = Patterns.PHONE.matcher(number);
            return match.matches();
        }

        /**
         * Contains all MMS messages in the MMS app inbox.
         */
        public static final class Inbox implements BaseMmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Inbox() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://mms-yang/inbox");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";
        }

        /**
         * Contains all MMS messages in the MMS app sent folder.
         */
        public static final class Sent implements BaseMmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Sent() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://mms-yang/sent");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";
        }

        /**
         * Contains all MMS messages in the MMS app drafts folder.
         */
        public static final class Draft implements BaseMmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Draft() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://mms-yang/drafts");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";
        }

        /**
         * Contains all MMS messages in the MMS app outbox.
         */
        public static final class Outbox implements BaseMmsColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Outbox() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.parse("content://mms-yang/outbox");

            /**
             * The default sort order for this table.
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";
        }

        /**
         * Contains address information for an MMS message.
         */
        public static final class Addr implements BaseColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Addr() {
            }

            /**
             * The ID of MM which this address entry belongs to. <P>Type: INTEGER (long)</P>
             */
            public static final String MSG_ID = "msg_id";

            /**
             * The ID of contact entry in Phone Book. <P>Type: INTEGER (long)</P>
             */
            public static final String CONTACT_ID = "contact_id";

            /**
             * The address text. <P>Type: TEXT</P>
             */
            public static final String ADDRESS = "address";

            /**
             * Type of address: must be one of {@code PduHeaders.BCC}, {@code PduHeaders.CC}, {@code PduHeaders.FROM}, {@code PduHeaders.TO}. <P>Type: INTEGER</P>
             */
            public static final String TYPE = "type";

            /**
             * Character set of this entry (MMS charset value). <P>Type: INTEGER</P>
             */
            public static final String CHARSET = "charset";
        }

        /**
         * Contains message parts.
         */
        public static final class Part implements BaseColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Part() {
            }

            /**
             * The identifier of the message which this part belongs to. <P>Type: INTEGER</P>
             */
            public static final String MSG_ID = "mid";

            /**
             * The order of the part. <P>Type: INTEGER</P>
             */
            public static final String SEQ = "seq";

            /**
             * The content type of the part. <P>Type: TEXT</P>
             */
            public static final String CONTENT_TYPE = "ct";

            /**
             * The name of the part. <P>Type: TEXT</P>
             */
            public static final String NAME = "name";

            /**
             * The charset of the part. <P>Type: TEXT</P>
             */
            public static final String CHARSET = "chset";

            /**
             * The file name of the part. <P>Type: TEXT</P>
             */
            public static final String FILENAME = "fn";

            /**
             * The content disposition of the part. <P>Type: TEXT</P>
             */
            public static final String CONTENT_DISPOSITION = "cd";

            /**
             * The content ID of the part. <P>Type: INTEGER</P>
             */
            public static final String CONTENT_ID = "cid";

            /**
             * The content location of the part. <P>Type: INTEGER</P>
             */
            public static final String CONTENT_LOCATION = "cl";

            /**
             * The start of content-type of the message. <P>Type: INTEGER</P>
             */
            public static final String CT_START = "ctt_s";

            /**
             * The type of content-type of the message. <P>Type: TEXT</P>
             */
            public static final String CT_TYPE = "ctt_t";

            /**
             * The location (on filesystem) of the binary data of the part. <P>Type: INTEGER</P>
             */
            public static final String _DATA = "_data";

            /**
             * The message text. <P>Type: TEXT</P>
             */
            public static final String TEXT = "text";
        }

        /**
         * Message send rate table.
         */
        public static final class Rate {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Rate() {
            }

            /**
             * The {@code content://} style URL for this table.
             */
            public static final Uri CONTENT_URI = Uri.withAppendedPath(Mms.CONTENT_URI, "rate");

            /**
             * When a message was successfully sent. <P>Type: INTEGER (long)</P>
             */
            public static final String SENT_TIME = "sent_time";
        }

        /**
         * Intents class.
         */
        public static final class Intents {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private Intents() {
            }

            /**
             * Indicates that the contents of specified URIs were changed. The application which is showing or caching these contents should be updated.
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String CONTENT_CHANGED_ACTION = "android.intent.action.CONTENT_CHANGED";

            /**
             * An extra field which stores the URI of deleted contents.
             */
            public static final String DELETED_CONTENTS = "deleted_contents";
        }
    }

    /**
     * Contains all MMS and SMS messages.
     */
    public static final class MmsSms implements BaseColumns {

        /**
         * Not instantiable.
         * 
         * @hide
         */
        private MmsSms() {
        }

        /**
         * The column to distinguish SMS and MMS messages in query results.
         */
        public static final String TYPE_DISCRIMINATOR_COLUMN = "transport_type";

        /**
         * The {@code content://} style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://mms-sms-yang/");

        /**
         * The {@code content://} style URL for this table, by conversation.
         */
        public static final Uri CONTENT_CONVERSATIONS_URI = Uri.parse("content://mms-sms-yang/conversations");

        /**
         * The {@code content://} style URL for this table, by phone number.
         */
        public static final Uri CONTENT_FILTER_BYPHONE_URI = Uri.parse("content://mms-sms-yang/messages/byphone");

        /**
         * The {@code content://} style URL for undelivered messages in this table.
         */
        public static final Uri CONTENT_UNDELIVERED_URI = Uri.parse("content://mms-sms-yang/undelivered");

        /**
         * The {@code content://} style URL for draft messages in this table.
         */
        public static final Uri CONTENT_DRAFT_URI = Uri.parse("content://mms-sms-yang/draft");

        /**
         * The {@code content://} style URL for locked messages in this table.
         */
        public static final Uri CONTENT_LOCKED_URI = Uri.parse("content://mms-sms-yang/locked");

        /**
         * Pass in a query parameter called "pattern" which is the text to search for. The sort order is fixed to be: {@code thread_id ASC, date DESC}.
         */
        public static final Uri SEARCH_URI = Uri.parse("content://mms-sms-yang/search");

        // Constants for message protocol types.

        /** SMS protocol type. */
        public static final int SMS_PROTO = 0;

        /** MMS protocol type. */
        public static final int MMS_PROTO = 1;

        // Constants for error types of pending messages.

        /** Error type: no error. */
        public static final int NO_ERROR = 0;

        /** Error type: generic transient error. */
        public static final int ERR_TYPE_GENERIC = 1;

        /** Error type: SMS protocol transient error. */
        public static final int ERR_TYPE_SMS_PROTO_TRANSIENT = 2;

        /** Error type: MMS protocol transient error. */
        public static final int ERR_TYPE_MMS_PROTO_TRANSIENT = 3;

        /** Error type: transport failure. */
        public static final int ERR_TYPE_TRANSPORT_FAILURE = 4;

        /** Error type: permanent error (along with all higher error values). */
        public static final int ERR_TYPE_GENERIC_PERMANENT = 10;

        /** Error type: SMS protocol permanent error. */
        public static final int ERR_TYPE_SMS_PROTO_PERMANENT = 11;

        /** Error type: MMS protocol permanent error. */
        public static final int ERR_TYPE_MMS_PROTO_PERMANENT = 12;

        /**
         * Contains pending messages info.
         */
        public static final class PendingMessages implements BaseColumns {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private PendingMessages() {
            }

            public static final Uri CONTENT_URI = Uri.withAppendedPath(MmsSms.CONTENT_URI, "pending");

            /**
             * The type of transport protocol (MMS or SMS). <P>Type: INTEGER</P>
             */
            public static final String PROTO_TYPE = "proto_type";

            /**
             * The ID of the message to be sent or downloaded. <P>Type: INTEGER (long)</P>
             */
            public static final String MSG_ID = "msg_id";

            /**
             * The type of the message to be sent or downloaded. This field is only valid for MM. For SM, its value is always set to 0. <P>Type: INTEGER</P>
             */
            public static final String MSG_TYPE = "msg_type";

            /**
             * The type of the error code. <P>Type: INTEGER</P>
             */
            public static final String ERROR_TYPE = "err_type";

            /**
             * The error code of sending/retrieving process. <P>Type: INTEGER</P>
             */
            public static final String ERROR_CODE = "err_code";

            /**
             * How many times we tried to send or download the message. <P>Type: INTEGER</P>
             */
            public static final String RETRY_INDEX = "retry_index";

            /**
             * The time to do next retry. <P>Type: INTEGER (long)</P>
             */
            public static final String DUE_TIME = "due_time";

            /**
             * The time we last tried to send or download the message. <P>Type: INTEGER (long)</P>
             */
            public static final String LAST_TRY = "last_try";
        }

        /**
         * Words table used by provider for full-text searches.
         * 
         * @hide
         */
        public static final class WordsTable {

            /**
             * Not instantiable.
             * 
             * @hide
             */
            private WordsTable() {
            }

            /**
             * Primary key. <P>Type: INTEGER (long)</P>
             */
            public static final String ID = "_id";

            /**
             * Source row ID. <P>Type: INTEGER (long)</P>
             */
            public static final String SOURCE_ROW_ID = "source_id";

            /**
             * Table ID (either 1 or 2). <P>Type: INTEGER</P>
             */
            public static final String TABLE_ID = "table_to_use";

            /**
             * The words to index. <P>Type: TEXT</P>
             */
            public static final String INDEXED_TEXT = "index_text";
        }
    }

}