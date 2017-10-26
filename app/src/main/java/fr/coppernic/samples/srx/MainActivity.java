package fr.coppernic.samples.srx;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.coppernic.sdk.ask.Defines;
import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.powermgmt.PowerMgmt;
import fr.coppernic.sdk.powermgmt.PowerMgmtFactory;
import fr.coppernic.sdk.utils.core.CpcBytes;
import fr.coppernic.sdk.utils.core.CpcDefinitions;
import fr.coppernic.sdk.utils.core.CpcResult;
import fr.coppernic.sdk.utils.io.InstanceListener;
import io.reactivex.Scheduler;

public class MainActivity extends AppCompatActivity implements PowerListener, InstanceListener<Reader> {

    @BindView(R.id.spAddress) Spinner spAddress;
    @BindView(R.id.spNbBytes) Spinner spNbBytes;
    @BindView(R.id.etDataToWrite) EditText etDataToWrite;
    @BindView(R.id.etLogs) EditText etLogs;

    private Reader reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initSpinners();
        PowerManager.get().registerListener(this);
    }

    private void initSpinners() {
        // Fills Address spinner
        List<String> list = new ArrayList<String>();

        for (int i = 0; i < 64; i++) {
            list.add(Integer.toString(i));
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAddress.setAdapter(dataAdapter);
        // Fills Nb bytes to read / write spinner
        List<String> listNbBytes = new ArrayList<String>();

        for (int i = 0; i < 64; i++) {
            listNbBytes.add(Integer.toString(i+1));
        }
        ArrayAdapter<String> dataAdapterNbBytes = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, listNbBytes);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNbBytes.setAdapter(dataAdapterNbBytes);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Powers on ASK RFID reader
        ConePeripheral.RFID_ASK_UCM108_GPIO.on(this);
    }

    @Override
    protected void onStop() {
        // Powers off ASK RFID reader
        ConePeripheral.RFID_ASK_UCM108_GPIO.off(this);
        super.onStop();
    }

    @Override
    public void onPowerUp(CpcResult.RESULT result, Peripheral peripheral) {
        // Initializes ASK reader
        Reader.getInstance(this, this);
    }

    @Override
    public void onPowerDown(CpcResult.RESULT result, Peripheral peripheral) {

    }

    @Override
    public void onCreated(Reader reader) {
        this.reader = reader;
        reader.cscOpen(CpcDefinitions.ASK_READER_PORT, 115200, false);
        SystemClock.sleep(500);
        StringBuilder sb = new StringBuilder();
        reader.cscVersionCsc(sb);
        Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisposed(Reader reader) {

    }

    @OnClick(R.id.btnActive)
    public void active() {
        byte[] chipType = new byte[1];
        byte[] uid = new byte[8];
        byte[] status = new byte[1];
        if (reader.srxActive(chipType, uid, status) == Defines.RCSC_Ok) {

            addLog ("Active: OK");
            addLog ("chipType: " + CpcBytes.byteArrayToString(chipType, chipType.length));
            addLog ("uid: " + CpcBytes.byteArrayToString(uid, uid.length));
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));

        } else {
            addLog("Active: failed");
        }
    }

    @OnClick(R.id.btnRelease)
    public void release () {
        byte[] status = new byte[1];

        if (reader.srxRelease((byte) 0x00, status) == Defines.RCSC_Ok) {

            addLog ("Release: OK");
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));

        } else {
            addLog("Release: failed");
        }
    }

    @OnClick(R.id.btnRead)
    public void read() {
        byte[] status = new byte[1];
        byte[] dataLength = new byte[1];
        byte[] data = new byte[256];

        // Gets input parameters
        short address = (short)spAddress.getSelectedItemPosition();
        byte nbBytesToRead = (byte)(spNbBytes.getSelectedItemPosition() + 1);

        if (reader.srxRead(address, nbBytesToRead, status, dataLength, data) == Defines.RCSC_Ok) {
            addLog ("Read: OK");
            addLog ("dataLength: " + CpcBytes.byteArrayToString(dataLength, dataLength.length));
            addLog ("data: " + CpcBytes.byteArrayToString(data, dataLength[0]));
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));
        } else {
            addLog ("Read: failed");
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));
        }
    }

    @OnClick(R.id.btnWrite)
    public void write() {
        byte[] status = new byte[1];
        byte[] dataLength = new byte[1];
        byte[] data = new byte[256];

        // Gets input parameters
        short address = (short)spAddress.getSelectedItemPosition();
        byte nbBytesToWrite = (byte)(spNbBytes.getSelectedItemPosition() + 1);

        byte[] dataToWrite = new byte[nbBytesToWrite];

        getDataToWrite(dataToWrite);

        if (reader.srxWrite(address, nbBytesToWrite, dataToWrite, status, dataLength, data) == Defines.RCSC_Ok) {
            addLog ("Write: OK");
            addLog ("dataLength: " + CpcBytes.byteArrayToString(dataLength, dataLength.length));
            addLog ("data written: " + CpcBytes.byteArrayToString(data, dataLength[0]));
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));
        } else {
            addLog ("Write: failed");
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));
        }
    }

    @OnClick(R.id.btnReadBlocks)
    public void readBlocks() {
        byte[] status = new byte[1];
        byte[] dataLength = new byte[1];
        byte[] data = new byte[256];

        // Gets input parameters
        byte block = (byte)spAddress.getSelectedItemPosition();
        byte nbBlocks = (byte)(spNbBytes.getSelectedItemPosition() + 1);

        if (reader.srxReadBlocks(block, nbBlocks, status, dataLength, data) == Defines.RCSC_Ok) {
            addLog ("ReadBlocks: OK");
            addLog ("dataLength: " + CpcBytes.byteArrayToString(dataLength, dataLength.length));
            addLog ("data: " + CpcBytes.byteArrayToString(data, dataLength[0]));
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));
        } else {
            addLog ("ReadBlocks: failed");
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));
        }
    }

    @OnClick(R.id.btnWriteBlocks)
    public void writeBlocks() {
        byte[] status = new byte[1];
        byte[] dataLength = new byte[1];
        byte[] data = new byte[256];

        // Gets input parameters
        byte block = (byte)spAddress.getSelectedItemPosition();
        byte nbBlocks = (byte)(spNbBytes.getSelectedItemPosition() + 1);

        byte sizeOfBlock = 4;
        byte dataToWriteLength = (byte) (nbBlocks * sizeOfBlock);
        byte[] dataToWrite = new byte[dataToWriteLength];

        getDataToWrite(dataToWrite);

        if (reader.srxWriteBlocks(block, nbBlocks, dataToWriteLength, dataToWrite, status, dataLength, data) == Defines.RCSC_Ok) {
            addLog ("WriteBlocks: OK");
            addLog ("dataLength: " + CpcBytes.byteArrayToString(dataLength, dataLength.length));
            addLog ("data written: " + CpcBytes.byteArrayToString(data, dataLength[0]));
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));
        } else {
            addLog ("WriteBlocks: failed");
            addLog ("status: " + CpcBytes.byteArrayToString(status, status.length));
        }
    }

    private void addLog (String log) {
        // Inserts text at the beginning
        etLogs.getText().insert(0, log + "\r\n");
        // Sets cursor in first position
        etLogs.setSelection(0);
    }

    private void getDataToWrite (byte[] dataToWrite) {
        String[] data = etDataToWrite.getText().toString().split(" ");

        for(int i = 0; i < data.length; i++) {
            try {
                dataToWrite[i] = (byte)Integer.parseInt(data[i], 16);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                addLog(ex.getMessage());
            }
        }
    }
}
