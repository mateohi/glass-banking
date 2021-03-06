package uy.infocorp.banking.glass.controller.account;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.widget.Slider;
import com.google.common.collect.Lists;

import java.util.List;

import uy.infocorp.banking.glass.R;
import uy.infocorp.banking.glass.controller.account.movements.LastMovementsActivity;
import uy.infocorp.banking.glass.controller.account.transactions.LastTransfersActivity;
import uy.infocorp.banking.glass.controller.auth.AuthenticableActivity;
import uy.infocorp.banking.glass.controller.common.product.GetProductsTask;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.framework.common.Product;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.framework.common.ProductType;
import uy.infocorp.banking.glass.util.async.FinishedTaskListener;
import uy.infocorp.banking.glass.util.serialization.EnumUtil;

public class ProductsBalanceActivity extends AuthenticableActivity {

    public static final String PRODUCT_BANK_IDENTIFIER = "productBankIdentifier";
    public static final String PRODUCT_ALIAS = "alias";
    public static final String AUTH_TOKEN = "authToken";

    private static final List<ProductType> STACKED_PRODUCTS =
            Lists.newArrayList(ProductType.currentAccount, ProductType.creditCard, ProductType.savingsAccount);

    private List<CardBuilder> cards = Lists.newArrayList();
    private List<Product> products = Lists.newArrayList();

    private Product selectedProduct;
    private Slider.Indeterminate slider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getAuthToken();
    }

    @Override
    protected void authenticationOk() {
        showInitialView();
        createCards();
    }

    @Override
    protected void authenticationError() {
        showAuthenticationErrorView();
        delayedFinish(3);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        switch (selectedProduct.getProductType()) {
            case creditCard:
                inflater.inflate(R.menu.credit_card_transaction_detail, menu);
                return true;
            case currentAccount:
                inflater.inflate(R.menu.account_transaction_detail, menu);
                return true;
            case savingsAccount:
                inflater.inflate(R.menu.account_transaction_detail, menu);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.get_last_movement:
                startLastMovementIntent();
                return true;
            case R.id.get_last_transfer:
                startLastTransferIntent();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startLastMovementIntent() {
        Intent intent = new Intent(this, LastMovementsActivity.class);
        intent.putExtra(PRODUCT_BANK_IDENTIFIER, selectedProduct.getProductBankIdentifier());
        intent.putExtra(PRODUCT_ALIAS, selectedProduct.getProductAlias());
        intent.putExtra(AUTH_TOKEN, this.authToken);
        EnumUtil.serialize(selectedProduct.getProductType()).to(intent);

        startActivity(intent);
    }

    private void startLastTransferIntent() {
        Intent intent = new Intent(this, LastTransfersActivity.class);
        intent.putExtra(PRODUCT_BANK_IDENTIFIER, selectedProduct.getProductBankIdentifier());
        intent.putExtra(PRODUCT_ALIAS, selectedProduct.getProductAlias());
        intent.putExtra(AUTH_TOKEN, this.authToken);
        EnumUtil.serialize(selectedProduct.getProductType()).to(intent);

        startActivity(intent);
    }

    private void showInitialView() {
        View initialView = new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Loading account ...")
                .setIcon(R.drawable.ic_sync)
                .getView();
        setContentView(initialView);

        this.slider = Slider.from(initialView).startIndeterminate();
    }

    private void showNoProductsView() {
        View initialView = new CardBuilder(this, CardBuilder.Layout.ALERT)
                .setText("No accounts registered yet")
                .setIcon(R.drawable.ic_help)
                .getView();

        setContentView(initialView);
    }

    private void showNoConnectivityView() {
        View initialView = new CardBuilder(this, CardBuilder.Layout.ALERT)
                .setText("Unable to get your accounts")
                .setFootnote("Check your internet connection")
                .setIcon(R.drawable.ic_cloud_sad_150)
                .getView();

        setContentView(initialView);
    }

    private void showAuthenticationErrorView() {
        View errorView = new CardBuilder(this, CardBuilder.Layout.ALERT)
                .setText("Authentication error")
                .setFootnote("Check your pin")
                .setIcon(R.drawable.ic_warning_150)
                .getView();

        setContentView(errorView);
    }

    private void createCards() {
        new GetProductsTask(new FinishedTaskListener<List<Product>>() {
            @Override
            public void onResult(List<Product> products) {
                slider.hide();
                slider = null;

                if (products == null) {
                    showNoConnectivityView();
                    delayedFinish(3);
                } else if (products.isEmpty()) {
                    showNoProductsView();
                    delayedFinish(3);
                } else {
                    ProductsBalanceActivity.this.products = products;

                    for (Product product : products) {
                        cards.add(createCard(product));
                    }
                    updateCardScrollView();
                }
            }
        }).execute(this.authToken);
    }


    private void updateCardScrollView() {
        ProductCardScrollAdapter adapter = new ProductCardScrollAdapter();

        CardScrollView cardScrollView = new CardScrollView(this);
        cardScrollView.setAdapter(adapter);
        cardScrollView.activate();
        cardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);

                selectedProduct = products.get(position);
                //this is due to the dependence of the menu options to the Product Type
                // of the selected product
                invalidateOptionsMenu();
                openOptionsMenu();
            }
        });

        setContentView(cardScrollView);
    }

    private CardBuilder createCard(Product product) {
        String alias = product.getProductAlias();
        String balance = product.getConsolidatedPositionBalance();
        String footnote = product.getProductNumber();
        String accountDescription = product.getProductTypeDescription();
        int iconId = product.getProductIconId();

        CardBuilder cardBuilder = new CardBuilder(this, CardBuilder.Layout.COLUMNS_FIXED)
                .setText(accountDescription + "\n" + alias + "\n" + balance)
                .setFootnote(footnote)
                .setIcon(iconId);

        if (STACKED_PRODUCTS.contains(product.getProductType())) {
            cardBuilder.showStackIndicator(true);
        }

        return cardBuilder;
    }

    private class ProductCardScrollAdapter extends CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return cards.indexOf(item);
        }

        @Override
        public int getCount() {
            return cards.size();
        }

        @Override
        public Object getItem(int position) {
            return cards.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position) {
            return cards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return cards.get(position).getView(convertView, parent);
        }
    }

}