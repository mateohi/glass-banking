package uy.infocorp.banking.glass.controller.transactions;

import android.util.Log;

import java.util.List;

import uy.infocorp.banking.glass.integration.privateapi.authentication.AuthenticationClient;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.framework.common.ProductType;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.transfers.Transfer;
import uy.infocorp.banking.glass.integration.privateapi.transfersHistory.TransferHistoryClient;
import uy.infocorp.banking.glass.util.async.FinishedTaskListener;
import uy.infocorp.banking.glass.util.async.SimpleAsyncTask;

public class GetLastTransfersTask extends SimpleAsyncTask<List<Transfer>> {

    private static final String TAG = GetLastTransfersTask.class.getSimpleName();

    public GetLastTransfersTask(FinishedTaskListener<List<Transfer>> listener) {
        super(listener);
    }

    @Override
    protected List<Transfer> doInBackground(Object... params) {
        try {
            String productBankIdentifier = (String)params[0];
            ProductType productType = (ProductType)params[1];
            String authToken = AuthenticationClient.instance().completeLogOn();
            return TransferHistoryClient.instance().getLastTransfers(authToken, productType,
                                                                     productBankIdentifier);
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException: Unable to get latest transactions -" + e.getMessage());
            return null;
        } catch (Exception exc) {
            Log.e(TAG, "Unable to get latest transactions -" + exc.getMessage());
            return null;
        }
    }
}
