/*
 * Copyright 2017 - 2024 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 - 2018 Andrey Kunitsyn (andrey@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.notificators;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.database.StatisticsManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.NotificationMessage;
import org.traccar.sms.SmsManager;

import javax.swing.event.MenuEvent;
import java.sql.Date;
import java.text.SimpleDateFormat;

@Singleton
public class NotificatorSms extends Notificator {

    private final SmsManager smsManager;
    private final StatisticsManager statisticsManager;

    @Inject
    public NotificatorSms(
            SmsManager smsManager, NotificationFormatter notificationFormatter, StatisticsManager statisticsManager) {
        super(notificationFormatter, "short");
        this.smsManager = smsManager;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void send(User user, NotificationMessage message, Device device , Event event, Position position) throws MessageException {
        if (device != null && device.getContact()  != null ){
            statisticsManager.registerSms();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String formattedDate = dateFormat.format(event.getEventTime());
            String messageBody = "http://maps.google.com/maps?q=" + position.getLatitude() + "," + position.getLongitude() +
                    " ID:" + device.getUniqueId() +
                    " Latitude:" + position.getLatitude() +
                    " Longitude:" + position.getLongitude() +
                    " Date Time:" + formattedDate +
                    " Speed:" + position.getSpeed();
            smsManager.sendMessage(device.getContact(), message.getBody(), false);
        } else if (user.getPhone() != null) {
            statisticsManager.registerSms();
            smsManager.sendMessage(user.getPhone(), message.getBody(), false);
        }
    }

}
