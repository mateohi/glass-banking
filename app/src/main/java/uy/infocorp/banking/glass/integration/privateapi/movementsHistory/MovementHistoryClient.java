package uy.infocorp.banking.glass.integration.privateapi.movementsHistory;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.Arrays;
import java.util.List;

import uy.infocorp.banking.glass.R;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.framework.common.ProductType;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.movements.Movement;
import uy.infocorp.banking.glass.integration.privateapi.movementsHistory.dto.MovementHistoryResponseDTO;
import uy.infocorp.banking.glass.util.http.BaseClient;
import uy.infocorp.banking.glass.util.http.RestExecutionBuilder;
import uy.infocorp.banking.glass.util.resources.Resources;

public class MovementHistoryClient extends BaseClient {

    private static final String TAG = MovementHistoryClient.class.getSimpleName();

    private static MovementHistoryClient instance;
    private RestExecutionBuilder builder;
    private String authToken;
    private String productBankIdentifier;
    private ProductType productType;

    private MovementHistoryClient() {
        builder = RestExecutionBuilder.get();
    }

    public static MovementHistoryClient instance() {
        if (instance == null) {
            instance = new MovementHistoryClient();
        }
        return instance;
    }

    public List<Movement> getLastMovements(ProductType productType,
                                           String productBankIdentifier) throws Exception {
        this.productType = productType;
        this.productBankIdentifier = productBankIdentifier;
        return (List<Movement>) this.execute();
    }


    @Override
    protected Object getOffline() {
        //check if credit cards or account
        Movement[] movements = Resources.jsonToObject(R.raw.movements,
                Movement[].class);
        return MovementHistoryUtils.getCorrectedMovements(movements);
    }

    @Override
    protected Object getOnline() {
        this.authToken = Resources.getAuthToken();
        String formattedUrl = MovementHistoryUtils.buildFormattedUrl();
        String xAuthTokenHeaderName = Resources.getString(R.string.x_auth_header);
        Header tokenHeader = new BasicHeader(xAuthTokenHeaderName, this.authToken);

        MovementHistoryResponseDTO response = this.builder
                .appendUrl(formattedUrl)
                .appendHeader(tokenHeader)
                .execute(MovementHistoryResponseDTO.class);

        Movement[] movements = response.getItems();

        return MovementHistoryUtils.getCorrectedMovements(movements);
    }
}
