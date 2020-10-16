package com.azoft.energosbyt.universal.service.queue;

import com.azoft.energosbyt.universal.dto.BaseMeter;
import com.azoft.energosbyt.universal.dto.BasePerson;
import com.azoft.energosbyt.universal.dto.BasePremise;
import com.azoft.energosbyt.universal.exception.ApiException;
import com.azoft.energosbyt.universal.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CcbService extends AbstractQueueService {

    private static final String TYPE_SEARCH_PERSON = "searchPerson";
    private static final String TYPE_SEARCH_METER = "searchMeter";
    private static final String TYPE_GET_METER = "getMeter";
    private static final String TYPE_GET_PERSON_ACCOUNT = "getPersAccount";
    private static final String TYPE_GET_PREMISE = "getPremise";

    @Value("${energosbyt.rabbit.request.check.queue-name}")
    private String ccbQueueName;

    public BasePerson searchAccountsByPersonId(String personId) {
        String replyQueueName = null;

        try {
            replyQueueName = declareReplyQueueWithUuidName();
            MessageProperties properties = createMessageProperties(replyQueueName, TYPE_GET_PERSON_ACCOUNT);
            BasePerson bodyObject = createSearchAccountByPersonIdRabbitRequest(personId);
            byte[] messageBody = toJsonToBytes(bodyObject);

            Message personRequestMessage = new Message(messageBody, properties);

            template.send(ccbQueueName, personRequestMessage);
            BasePerson response = safelyReceiveResponse(replyQueueName, BasePerson.class);
            log.info("response for searchAccount: {}", response);
            return response;

        } finally {
            if (replyQueueName != null) {
                rabbitAdmin.deleteQueue(replyQueueName);
            }
        }
    }

    public String getAddress(String account) {
        BasePerson person = searchPersonByAccount(account);
        BasePerson accountsSearchResult = searchAccountsByPersonId(person.getId());

        String premiseId = accountsSearchResult.getAccounts().stream()
                .filter(acc -> account.equals(acc.getAccount_number()))
                .findFirst()
                .map(BasePerson.Account::getPremise)
                .map(BasePerson.Account.AccountPrem::getPremise_id)
                .orElse(null);

        if (premiseId == null) {
            return null;
        }

        BasePremise prem = getPremise(premiseId);
        String address = (prem.getPostal() + " " + prem.getCity() + " " + prem.getCounty() +
                " " + prem.getStreet() + " " + prem.getHouse() + "-" + prem.getApartment())
                .replaceAll("null", "")
                .replaceAll(" ", " ");
        return address;
    }

    public BasePremise getPremise(String premiseId) {

        String replyQueueName = null;
        try {
            replyQueueName = declareReplyQueueWithUuidName();
            MessageProperties properties = createMessageProperties(replyQueueName, TYPE_GET_PREMISE);
            BasePremise bodyObject = createGetPremiseRabbitRequest(premiseId);
            byte[] messageBody = toJsonToBytes(bodyObject);

            Message personRequestMessage = new Message(messageBody, properties);

            template.send(ccbQueueName, personRequestMessage);
            BasePremise response = safelyReceiveResponse(replyQueueName, BasePremise.class);
            log.info("response for getPremise: {}", response);
            return response;
        } finally {
            if (replyQueueName != null) {
                rabbitAdmin.deleteQueue(replyQueueName);
            }
        }
    }

    private BasePremise createGetPremiseRabbitRequest(String premiseId) {
        BasePremise premise = new BasePremise();
        premise.setId(premiseId);
        return premise;
    }

    public BasePerson searchPersonByAccount(String account) {

        String personReplyQueueName = null;

        try {
            personReplyQueueName = declareReplyQueueWithUuidName();
            MessageProperties personMessageProperties = createMessageProperties(personReplyQueueName, TYPE_SEARCH_PERSON);
            BasePerson bodyObject = createSearchPersonByAccountRabbitRequest(account);
            byte[] personMessageBody = toJsonToBytes(bodyObject);
            Message personRequestMessage = new Message(personMessageBody, personMessageProperties);

            template.send(ccbQueueName, personRequestMessage);
            BasePerson personRabbitResponse = safelyReceiveResponse(personReplyQueueName, BasePerson.class);

            if (personRabbitResponse.getSrch_res().getRes().isEmpty()) {
                String message = "No person found for account id = " + account;
                log.error(message);
                throw new ApiException(message, ErrorCode.UNEXPECTED_ERROR, true);
            }
            return personRabbitResponse;
        } finally {
            if (personReplyQueueName != null) {
                rabbitAdmin.deleteQueue(personReplyQueueName);
            }
        }
    }

    public BaseMeter searchMetersByPersonId(String personId) {

        String metersReplyQueueName = null;

        try {
            metersReplyQueueName = declareReplyQueueWithUuidName();
            MessageProperties metersMessageProperties = createMessageProperties(metersReplyQueueName, TYPE_SEARCH_METER);
            BaseMeter bodyObject = createMetersRabbitRequest(personId);
            byte[] metersMessageBody = toJsonToBytes(bodyObject);
            Message metersRequestMessage = new Message(metersMessageBody, metersMessageProperties);

            template.send(ccbQueueName, metersRequestMessage);

            return safelyReceiveResponse(metersReplyQueueName, BaseMeter.class);
        } finally {
            if (metersReplyQueueName != null) {
                rabbitAdmin.deleteQueue(metersReplyQueueName);
            }
        }
    }

    public BaseMeter getMeterById(String meterId) {

        String meterValuesReplyQueueName = null;

        try {
            meterValuesReplyQueueName = declareReplyQueueWithUuidName();
            MessageProperties metersMessageProperties = createMessageProperties(meterValuesReplyQueueName, TYPE_GET_METER);
            BaseMeter bodyObject = createMeterValuesRabbitRequest(meterId);
            byte[] metersMessageBody = toJsonToBytes(bodyObject);
            Message meterValuesRequestMessage = new Message(metersMessageBody, metersMessageProperties);

            template.send(ccbQueueName, meterValuesRequestMessage);

            return safelyReceiveResponse(meterValuesReplyQueueName, BaseMeter.class);
        } finally {
            if (meterValuesReplyQueueName != null) {
                rabbitAdmin.deleteQueue(meterValuesReplyQueueName);
            }
        }
    }

    private BaseMeter createMeterValuesRabbitRequest(String meterId) {
        BaseMeter rabbitRequest = new BaseMeter();
        rabbitRequest.setSystem_id(thisSystemId);
        rabbitRequest.setId(meterId);
        return rabbitRequest;
    }

    private BaseMeter createMetersRabbitRequest(String personId) {
        BaseMeter rabbitRequest = new BaseMeter();
        rabbitRequest.setSystem_id(thisSystemId);

        BaseMeter.Srch search = new BaseMeter.Srch();
        search.setPerson_Id(personId);
        rabbitRequest.setSrch(search);
        return rabbitRequest;
    }

    private BasePerson createSearchPersonByAccountRabbitRequest(String account) {
        BasePerson rabbitRequest = new BasePerson();
        rabbitRequest.setSystem_id(thisSystemId);

        BasePerson.Srch search = new BasePerson.Srch();
        search.setAccount_number(account);
        search.setDept("ORESB");
        rabbitRequest.setSrch(search);
        return rabbitRequest;
    }

    private BasePerson createSearchAccountByPersonIdRabbitRequest(String personId) {
        BasePerson basePerson = new BasePerson();
        basePerson.setId(personId);
        return basePerson;
    }
}
