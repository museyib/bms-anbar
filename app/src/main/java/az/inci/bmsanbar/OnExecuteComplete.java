package az.inci.bmsanbar;

import az.inci.bmsanbar.model.v2.ResponseMessage;

public interface OnExecuteComplete
{
    void executeComplete(ResponseMessage message);
}
