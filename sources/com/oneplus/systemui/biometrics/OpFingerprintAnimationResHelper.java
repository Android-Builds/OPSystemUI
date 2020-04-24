package com.oneplus.systemui.biometrics;

import android.content.Context;
import com.android.systemui.R$drawable;
import com.android.systemui.R$integer;

public class OpFingerprintAnimationResHelper {
    private static final int[] DOWN_ANIMATION_02 = {R$drawable.fod02_00, R$drawable.fod02_01, R$drawable.fod02_02, R$drawable.fod02_03, R$drawable.fod02_04, R$drawable.fod02_05, R$drawable.fod02_06, R$drawable.fod02_07, R$drawable.fod02_08, R$drawable.fod02_09, R$drawable.fod02_10, R$drawable.fod02_11, R$drawable.fod02_12, R$drawable.fod02_13, R$drawable.fod02_14, R$drawable.fod02_15, R$drawable.fod02_16, R$drawable.fod02_17, R$drawable.fod02_18, R$drawable.fod02_19, R$drawable.fod02_20, R$drawable.fod02_21, R$drawable.fod02_22, R$drawable.fod02_23, R$drawable.fod02_24, R$drawable.fod02_25, R$drawable.fod02_26, R$drawable.fod02_27, R$drawable.fod02_28, R$drawable.fod02_29, R$drawable.fod02_30, R$drawable.fod02_31, R$drawable.fod02_32, R$drawable.fod02_33, R$drawable.fod02_34, R$drawable.fod02_35, R$drawable.fod02_36, R$drawable.fod02_37, R$drawable.fod02_38, R$drawable.fod02_39, R$drawable.fod02_40, R$drawable.fod02_41, R$drawable.fod02_42, R$drawable.fod02_43, R$drawable.fod02_44, R$drawable.fod02_45, R$drawable.fod02_46, R$drawable.fod02_47, R$drawable.fod02_48, R$drawable.fod02_49, R$drawable.fod02_50, R$drawable.fod02_51, R$drawable.fod02_52, R$drawable.fod02_53, R$drawable.fod02_54, R$drawable.fod02_55, R$drawable.fod02_56, R$drawable.fod02_57, R$drawable.fod02_58, R$drawable.fod02_59, R$drawable.fod02_60, R$drawable.fod02_61, R$drawable.fod02_62, R$drawable.fod02_63, R$drawable.fod02_64, R$drawable.fod02_65, R$drawable.fod02_66, R$drawable.fod02_67, R$drawable.fod02_68, R$drawable.fod02_69, R$drawable.fod02_70, R$drawable.fod02_71, R$drawable.fod02_72, R$drawable.fod02_73, R$drawable.fod02_74, R$drawable.fod02_75, R$drawable.fod02_76, R$drawable.fod02_77, R$drawable.fod02_78, R$drawable.fod02_79, R$drawable.fod02_80, R$drawable.fod02_81, R$drawable.fod02_82, R$drawable.fod02_83, R$drawable.fod02_84, R$drawable.fod02_85, R$drawable.fod02_86, R$drawable.fod02_87, R$drawable.fod02_88, R$drawable.fod02_89, R$drawable.fod02_90, R$drawable.fod02_91, R$drawable.fod02_92, R$drawable.fod02_93, R$drawable.fod02_94, R$drawable.fod02_95, R$drawable.fod02_96, R$drawable.fod02_97, R$drawable.fod02_98, R$drawable.fod02_99};
    private static final int[] DOWN_ANIMATION_03 = {R$drawable.fod03_00, R$drawable.fod03_01, R$drawable.fod03_02, R$drawable.fod03_03, R$drawable.fod03_04, R$drawable.fod03_05, R$drawable.fod03_06, R$drawable.fod03_07, R$drawable.fod03_08, R$drawable.fod03_09, R$drawable.fod03_10, R$drawable.fod03_11, R$drawable.fod03_12, R$drawable.fod03_13, R$drawable.fod03_14, R$drawable.fod03_15, R$drawable.fod03_16, R$drawable.fod03_17, R$drawable.fod03_18, R$drawable.fod03_19, R$drawable.fod03_20, R$drawable.fod03_21, R$drawable.fod03_22, R$drawable.fod03_23, R$drawable.fod03_24, R$drawable.fod03_25, R$drawable.fod03_26, R$drawable.fod03_27, R$drawable.fod03_28, R$drawable.fod03_29, R$drawable.fod03_30, R$drawable.fod03_31, R$drawable.fod03_32, R$drawable.fod03_33, R$drawable.fod03_34, R$drawable.fod03_35, R$drawable.fod03_36, R$drawable.fod03_37, R$drawable.fod03_38, R$drawable.fod03_39, R$drawable.fod03_40, R$drawable.fod03_41, R$drawable.fod03_42, R$drawable.fod03_43, R$drawable.fod03_44, R$drawable.fod03_45, R$drawable.fod03_46, R$drawable.fod03_47, R$drawable.fod03_48, R$drawable.fod03_49, R$drawable.fod03_50, R$drawable.fod03_51, R$drawable.fod03_52, R$drawable.fod03_53, R$drawable.fod03_54, R$drawable.fod03_55, R$drawable.fod03_56, R$drawable.fod03_57, R$drawable.fod03_58, R$drawable.fod03_59, R$drawable.fod03_60, R$drawable.fod03_61, R$drawable.fod03_62, R$drawable.fod03_63, R$drawable.fod03_64, R$drawable.fod03_65, R$drawable.fod03_66, R$drawable.fod03_67, R$drawable.fod03_68, R$drawable.fod03_69, R$drawable.fod03_70, R$drawable.fod03_71, R$drawable.fod03_72, R$drawable.fod03_73, R$drawable.fod03_74, R$drawable.fod03_75, R$drawable.fod03_76, R$drawable.fod03_77, R$drawable.fod03_78, R$drawable.fod03_79, R$drawable.fod03_80, R$drawable.fod03_81, R$drawable.fod03_82, R$drawable.fod03_83, R$drawable.fod03_84, R$drawable.fod03_85, R$drawable.fod03_86, R$drawable.fod03_87, R$drawable.fod03_88, R$drawable.fod03_89, R$drawable.fod03_90, R$drawable.fod03_91, R$drawable.fod03_92, R$drawable.fod03_93, R$drawable.fod03_94, R$drawable.fod03_95, R$drawable.fod03_96, R$drawable.fod03_97, R$drawable.fod03_98, R$drawable.fod03_99};
    private static final int[] DOWN_ANIMATION_DEFAULT = {R$drawable.fod_default_00, R$drawable.fod_default_01, R$drawable.fod_default_02, R$drawable.fod_default_03, R$drawable.fod_default_04, R$drawable.fod_default_05, R$drawable.fod_default_06, R$drawable.fod_default_07, R$drawable.fod_default_08, R$drawable.fod_default_09, R$drawable.fod_default_10, R$drawable.fod_default_11, R$drawable.fod_default_12, R$drawable.fod_default_13, R$drawable.fod_default_14, R$drawable.fod_default_15, R$drawable.fod_default_16, R$drawable.fod_default_17, R$drawable.fod_default_18, R$drawable.fod_default_19, R$drawable.fod_default_20, R$drawable.fod_default_21, R$drawable.fod_default_22, R$drawable.fod_default_23, R$drawable.fod_default_24, R$drawable.fod_default_25, R$drawable.fod_default_26, R$drawable.fod_default_27, R$drawable.fod_default_28, R$drawable.fod_default_29, R$drawable.fod_default_30, R$drawable.fod_default_31, R$drawable.fod_default_32, R$drawable.fod_default_33, R$drawable.fod_default_34, R$drawable.fod_default_35, R$drawable.fod_default_36, R$drawable.fod_default_37, R$drawable.fod_default_38, R$drawable.fod_default_39, R$drawable.fod_default_40, R$drawable.fod_default_41, R$drawable.fod_default_42, R$drawable.fod_default_43, R$drawable.fod_default_44, R$drawable.fod_default_45, R$drawable.fod_default_46, R$drawable.fod_default_47, R$drawable.fod_default_48, R$drawable.fod_default_49, R$drawable.fod_default_50, R$drawable.fod_default_51, R$drawable.fod_default_52, R$drawable.fod_default_53, R$drawable.fod_default_54, R$drawable.fod_default_55, R$drawable.fod_default_56, R$drawable.fod_default_57, R$drawable.fod_default_58, R$drawable.fod_default_59, R$drawable.fod_default_60, R$drawable.fod_default_61, R$drawable.fod_default_62, R$drawable.fod_default_63, R$drawable.fod_default_64, R$drawable.fod_default_65, R$drawable.fod_default_66, R$drawable.fod_default_67, R$drawable.fod_default_68, R$drawable.fod_default_69, R$drawable.fod_default_70, R$drawable.fod_default_71, R$drawable.fod_default_72, R$drawable.fod_default_73, R$drawable.fod_default_74, R$drawable.fod_default_75, R$drawable.fod_default_76, R$drawable.fod_default_77, R$drawable.fod_default_78, R$drawable.fod_default_79, R$drawable.fod_default_80, R$drawable.fod_default_81, R$drawable.fod_default_82, R$drawable.fod_default_83, R$drawable.fod_default_84, R$drawable.fod_default_85, R$drawable.fod_default_86, R$drawable.fod_default_87, R$drawable.fod_default_88, R$drawable.fod_default_89, R$drawable.fod_default_90, R$drawable.fod_default_91, R$drawable.fod_default_92, R$drawable.fod_default_93, R$drawable.fod_default_94, R$drawable.fod_default_95, R$drawable.fod_default_96, R$drawable.fod_default_97, R$drawable.fod_default_98, R$drawable.fod_default_99};
    private static final int[] DOWN_ANIMATION_MCL = {R$drawable.fod_mc_00, R$drawable.fod_mc_01, R$drawable.fod_mc_02, R$drawable.fod_mc_03, R$drawable.fod_mc_04, R$drawable.fod_mc_05, R$drawable.fod_mc_06, R$drawable.fod_mc_07, R$drawable.fod_mc_08, R$drawable.fod_mc_09, R$drawable.fod_mc_10, R$drawable.fod_mc_11, R$drawable.fod_mc_12, R$drawable.fod_mc_13, R$drawable.fod_mc_14, R$drawable.fod_mc_15, R$drawable.fod_mc_16, R$drawable.fod_mc_17, R$drawable.fod_mc_18, R$drawable.fod_mc_19, R$drawable.fod_mc_20, R$drawable.fod_mc_21, R$drawable.fod_mc_22, R$drawable.fod_mc_23, R$drawable.fod_mc_24, R$drawable.fod_mc_25, R$drawable.fod_mc_26, R$drawable.fod_mc_27, R$drawable.fod_mc_28, R$drawable.fod_mc_29, R$drawable.fod_mc_30, R$drawable.fod_mc_31, R$drawable.fod_mc_32, R$drawable.fod_mc_33, R$drawable.fod_mc_34, R$drawable.fod_mc_35, R$drawable.fod_mc_36, R$drawable.fod_mc_37, R$drawable.fod_mc_38, R$drawable.fod_mc_39, R$drawable.fod_mc_40, R$drawable.fod_mc_41, R$drawable.fod_mc_42, R$drawable.fod_mc_43, R$drawable.fod_mc_44, R$drawable.fod_mc_45, R$drawable.fod_mc_46, R$drawable.fod_mc_47, R$drawable.fod_mc_48, R$drawable.fod_mc_49, R$drawable.fod_mc_50, R$drawable.fod_mc_51, R$drawable.fod_mc_52, R$drawable.fod_mc_53, R$drawable.fod_mc_54, R$drawable.fod_mc_55, R$drawable.fod_mc_56, R$drawable.fod_mc_57, R$drawable.fod_mc_58, R$drawable.fod_mc_59};
    private static final int[] DOWN_CUST01_ANIMATION = {R$drawable.fod_cust01_anim_00, R$drawable.fod_cust01_anim_01, R$drawable.fod_cust01_anim_02, R$drawable.fod_cust01_anim_03, R$drawable.fod_cust01_anim_04, R$drawable.fod_cust01_anim_05, R$drawable.fod_cust01_anim_06, R$drawable.fod_cust01_anim_07, R$drawable.fod_cust01_anim_08, R$drawable.fod_cust01_anim_09, R$drawable.fod_cust01_anim_10, R$drawable.fod_cust01_anim_11, R$drawable.fod_cust01_anim_12, R$drawable.fod_cust01_anim_13, R$drawable.fod_cust01_anim_14, R$drawable.fod_cust01_anim_15, R$drawable.fod_cust01_anim_16, R$drawable.fod_cust01_anim_17, R$drawable.fod_cust01_anim_18, R$drawable.fod_cust01_anim_19, R$drawable.fod_cust01_anim_20, R$drawable.fod_cust01_anim_21, R$drawable.fod_cust01_anim_22, R$drawable.fod_cust01_anim_23, R$drawable.fod_cust01_anim_24, R$drawable.fod_cust01_anim_25, R$drawable.fod_cust01_anim_26, R$drawable.fod_cust01_anim_27, R$drawable.fod_cust01_anim_28, R$drawable.fod_cust01_anim_29, R$drawable.fod_cust01_anim_30, R$drawable.fod_cust01_anim_31, R$drawable.fod_cust01_anim_32, R$drawable.fod_cust01_anim_33, R$drawable.fod_cust01_anim_34, R$drawable.fod_cust01_anim_35, R$drawable.fod_cust01_anim_36, R$drawable.fod_cust01_anim_37, R$drawable.fod_cust01_anim_38, R$drawable.fod_cust01_anim_39, R$drawable.fod_cust01_anim_40, R$drawable.fod_cust01_anim_41, R$drawable.fod_cust01_anim_42, R$drawable.fod_cust01_anim_43, R$drawable.fod_cust01_anim_44, R$drawable.fod_cust01_anim_45, R$drawable.fod_cust01_anim_46, R$drawable.fod_cust01_anim_47, R$drawable.fod_cust01_anim_48, R$drawable.fod_cust01_anim_49, R$drawable.fod_cust01_anim_50, R$drawable.fod_cust01_anim_51, R$drawable.fod_cust01_anim_52, R$drawable.fod_cust01_anim_53, R$drawable.fod_cust01_anim_54, R$drawable.fod_cust01_anim_55, R$drawable.fod_cust01_anim_56, R$drawable.fod_cust01_anim_57, R$drawable.fod_cust01_anim_58, R$drawable.fod_cust01_anim_59, R$drawable.fod_cust01_anim_60, R$drawable.fod_cust01_anim_61, R$drawable.fod_cust01_anim_62, R$drawable.fod_cust01_anim_63, R$drawable.fod_cust01_anim_64, R$drawable.fod_cust01_anim_65, R$drawable.fod_cust01_anim_66, R$drawable.fod_cust01_anim_67, R$drawable.fod_cust01_anim_68, R$drawable.fod_cust01_anim_69, R$drawable.fod_cust01_anim_70, R$drawable.fod_cust01_anim_71, R$drawable.fod_cust01_anim_72, R$drawable.fod_cust01_anim_73, R$drawable.fod_cust01_anim_74, R$drawable.fod_cust01_anim_75, R$drawable.fod_cust01_anim_76, R$drawable.fod_cust01_anim_77, R$drawable.fod_cust01_anim_78, R$drawable.fod_cust01_anim_79, R$drawable.fod_cust01_anim_80, R$drawable.fod_cust01_anim_81, R$drawable.fod_cust01_anim_82, R$drawable.fod_cust01_anim_83, R$drawable.fod_cust01_anim_84, R$drawable.fod_cust01_anim_85, R$drawable.fod_cust01_anim_86, R$drawable.fod_cust01_anim_87, R$drawable.fod_cust01_anim_88, R$drawable.fod_cust01_anim_89, R$drawable.fod_cust01_anim_90, R$drawable.fod_cust01_anim_91, R$drawable.fod_cust01_anim_92, R$drawable.fod_cust01_anim_93, R$drawable.fod_cust01_anim_94, R$drawable.fod_cust01_anim_95, R$drawable.fod_cust01_anim_96, R$drawable.fod_cust01_anim_97, R$drawable.fod_cust01_anim_98, R$drawable.fod_cust01_anim_99};
    private static final int[] UP_ANIMATION_DEFAULT = {R$drawable.fod_default_f_00, R$drawable.fod_default_f_01, R$drawable.fod_default_f_02, R$drawable.fod_default_f_03, R$drawable.fod_default_f_04, R$drawable.fod_default_f_05, R$drawable.fod_default_f_06, R$drawable.fod_default_f_07, R$drawable.fod_default_f_08, R$drawable.fod_default_f_09, R$drawable.fod_default_f_10, R$drawable.fod_default_f_11, R$drawable.fod_default_f_12, R$drawable.fod_default_f_13, R$drawable.fod_default_f_14, R$drawable.fod_default_f_15, R$drawable.fod_default_f_16, R$drawable.fod_default_f_17, R$drawable.fod_default_f_18, R$drawable.fod_default_f_19, R$drawable.fod_default_f_20, R$drawable.fod_default_f_21, R$drawable.fod_default_f_22, R$drawable.fod_default_f_23, R$drawable.fod_default_f_24, R$drawable.fod_default_f_25, R$drawable.fod_default_f_26, R$drawable.fod_default_f_27, R$drawable.fod_default_f_28, R$drawable.fod_default_f_29, R$drawable.fod_default_f_30, R$drawable.fod_default_f_31, R$drawable.fod_default_f_32, R$drawable.fod_default_f_33, R$drawable.fod_default_f_34, R$drawable.fod_default_f_35, R$drawable.fod_default_f_36, R$drawable.fod_default_f_37, R$drawable.fod_default_f_38, R$drawable.fod_default_f_39, R$drawable.fod_default_f_40, R$drawable.fod_default_f_41, R$drawable.fod_default_f_42, R$drawable.fod_default_f_43, R$drawable.fod_default_f_44, R$drawable.fod_default_f_45, R$drawable.fod_default_f_46, R$drawable.fod_default_f_47, R$drawable.fod_default_f_48, R$drawable.fod_default_f_49, R$drawable.fod_default_f_50, R$drawable.fod_default_f_51, R$drawable.fod_default_f_52, R$drawable.fod_default_f_53, R$drawable.fod_default_f_54, R$drawable.fod_default_f_55, R$drawable.fod_default_f_56, R$drawable.fod_default_f_57, R$drawable.fod_default_f_58, R$drawable.fod_default_f_59, R$drawable.fod_default_f_60, R$drawable.fod_default_f_61, R$drawable.fod_default_f_62, R$drawable.fod_default_f_63, R$drawable.fod_default_f_64, R$drawable.fod_default_f_65, R$drawable.fod_default_f_66, R$drawable.fod_default_f_67, R$drawable.fod_default_f_68, R$drawable.fod_default_f_69, R$drawable.fod_default_f_70, R$drawable.fod_default_f_71, R$drawable.fod_default_f_72, R$drawable.fod_default_f_73, R$drawable.fod_default_f_74, R$drawable.fod_default_f_75, R$drawable.fod_default_f_76, R$drawable.fod_default_f_77, R$drawable.fod_default_f_78, R$drawable.fod_default_f_79, R$drawable.fod_default_f_80, R$drawable.fod_default_f_81, R$drawable.fod_default_f_82, R$drawable.fod_default_f_83, R$drawable.fod_default_f_84, R$drawable.fod_default_f_85, R$drawable.fod_default_f_86, R$drawable.fod_default_f_87, R$drawable.fod_default_f_88, R$drawable.fod_default_f_89, R$drawable.fod_default_f_90, R$drawable.fod_default_f_91, R$drawable.fod_default_f_92, R$drawable.fod_default_f_93, R$drawable.fod_default_f_94, R$drawable.fod_default_f_95, R$drawable.fod_default_f_96, R$drawable.fod_default_f_97, R$drawable.fod_default_f_98, R$drawable.fod_default_f_99};

    public static int[] getDownAnimationRes(Context context, int i) {
        if (i == 0) {
            return DOWN_ANIMATION_DEFAULT;
        }
        if (i == 1) {
            return DOWN_ANIMATION_02;
        }
        if (i == 2) {
            return DOWN_ANIMATION_03;
        }
        if (i == 3) {
            return DOWN_ANIMATION_MCL;
        }
        if (i == 9) {
            return null;
        }
        if (i == 10) {
            return DOWN_ANIMATION_MCL;
        }
        if (i == 11) {
            return DOWN_CUST01_ANIMATION;
        }
        return DOWN_ANIMATION_DEFAULT;
    }

    public static int[] getUpAnimationRes(int i) {
        return i == 0 ? UP_ANIMATION_DEFAULT : new int[0];
    }

    public static int getDownStartFrameIndex(Context context, int i) {
        if (i == 0) {
            return context.getResources().getInteger(R$integer.fod_default_down_anim_start_frame);
        }
        if (i == 1) {
            return context.getResources().getInteger(R$integer.fod_02_anim_start_frame);
        }
        if (i == 2) {
            return context.getResources().getInteger(R$integer.fod_03_anim_start_frame);
        }
        if (i == 3) {
            return context.getResources().getInteger(R$integer.fod_mcl_anim_start_frame);
        }
        if (i == 9) {
            return -1;
        }
        if (i == 10) {
            return context.getResources().getInteger(R$integer.fod_mcl_anim_start_frame);
        }
        if (i == 11) {
            return context.getResources().getInteger(R$integer.fod_cust01_anim_start_frame);
        }
        return context.getResources().getInteger(R$integer.fod_default_down_anim_start_frame);
    }

    public static int getDownEndFrameIndex(Context context, int i) {
        if (i == 0) {
            return context.getResources().getInteger(R$integer.fod_default_down_anim_end_frame);
        }
        if (i == 1) {
            return context.getResources().getInteger(R$integer.fod_02_anim_end_frame);
        }
        if (i == 2) {
            return context.getResources().getInteger(R$integer.fod_03_anim_end_frame);
        }
        if (i == 3) {
            return context.getResources().getInteger(R$integer.fod_mcl_anim_end_frame);
        }
        if (i == 9) {
            return -1;
        }
        if (i == 10) {
            return context.getResources().getInteger(R$integer.fod_mcl_anim_end_frame);
        }
        if (i == 11) {
            return context.getResources().getInteger(R$integer.fod_cust01_anim_end_frame);
        }
        return context.getResources().getInteger(R$integer.fod_default_down_anim_end_frame);
    }

    public static int getDownPlayFrameNum(Context context, int i) {
        int downStartFrameIndex = getDownStartFrameIndex(context, i);
        int downEndFrameIndex = getDownEndFrameIndex(context, i);
        if (downStartFrameIndex < 0 || downEndFrameIndex < 0 || downStartFrameIndex > downEndFrameIndex) {
            if (i == 0) {
                return DOWN_ANIMATION_DEFAULT.length;
            }
            if (i == 1) {
                return DOWN_ANIMATION_02.length;
            }
            if (i == 2) {
                return DOWN_ANIMATION_03.length;
            }
            if (i == 3) {
                return DOWN_ANIMATION_MCL.length;
            }
            if (i == 9) {
                return 0;
            }
            if (i == 10) {
                return DOWN_ANIMATION_MCL.length;
            }
            if (i == 11) {
                return DOWN_CUST01_ANIMATION.length;
            }
            return DOWN_ANIMATION_DEFAULT.length;
        } else if (downStartFrameIndex == downEndFrameIndex) {
            return 0;
        } else {
            return (downEndFrameIndex - downStartFrameIndex) + 1;
        }
    }

    public static int getUpStartFrameIndex(Context context, int i) {
        if (i == 0) {
            return context.getResources().getInteger(R$integer.fod_default_up_anim_start_frame);
        }
        return 0;
    }

    public static int getUpEndFrameIndex(Context context, int i) {
        if (i == 0) {
            return context.getResources().getInteger(R$integer.fod_default_up_anim_end_frame);
        }
        return 0;
    }

    public static int getUpPlayFrameNum(Context context, int i) {
        if (i != 0) {
            return 0;
        }
        int upStartFrameIndex = getUpStartFrameIndex(context, i);
        int upEndFrameIndex = getUpEndFrameIndex(context, i);
        if (upStartFrameIndex < 0 || upEndFrameIndex < 0 || upStartFrameIndex > upEndFrameIndex) {
            return UP_ANIMATION_DEFAULT.length;
        }
        if (upStartFrameIndex == upEndFrameIndex) {
            return 0;
        }
        return (upEndFrameIndex - upStartFrameIndex) + 1;
    }
}
